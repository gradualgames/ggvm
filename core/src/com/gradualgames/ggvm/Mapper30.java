package com.gradualgames.ggvm;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 6/10/2017.
 *
 * This class imitates the behavior of mapper 30, UnROM 512. It contains n
 * number of swappable Roms and one fixed rom, mapped to 0x8000 and
 * 0xc000 base addresses just like the real Mapper 30. Any write performed
 * to it is interpreted as a bankswitch command and it change out which of
 * the swappable roms is currently pointed to by the UnromSwitchboard object.
 *
 * This mapper also supports CHR-RAM bankswitching via bits 5 and 6 of any
 * write to the bus at range 0x8000 - 0xbfff.
 *
 * Please note that currently, CHR-RAM is write only. GGVm picks up the writes
 * and translates the writes into real texture data. There is only one 8kb chunk
 * of CHR-RAM on the ppu bus currently, so reading back CHR-RAM will not yet work.
 * It would not take much more work to wire this up however.
 * TODO: Allow CHR-RAM to be read via $2007 as well as written to regardless of
 * TODO: which chr-ram bank is swapped in. Right now you'll only get the data
 * TODO: written to the most recently selected bank.
 *
 * Finally this mapper supports selecting the current nametable as 0 or 1
 * via bit 7 of any write to the bus at the same range.
 */
public class Mapper30 implements ReadWriteRangeProvider {

    private static final int MAPPER_30_ROM_SIZE = 0x4000;
    private static final int MAPPER_30_CHR_RAM_BASE_ADDRESS = 0x0000;
    private static final int MAPPER_30_CHR_RAM_SIZE = 0x2000;
    private static final int SWAPPABLE_ROM_BASE_ADDRESS = 0x8000;
    private static final int FIXED_ROM_BASE_ADDRESS = 0xc000;

    private UnromSwitchboard swappableRoms;
    private Rom fixedRom;
    private Ram chrRam;
    private List<ReadWriteRange> nameTableRams = new ArrayList<ReadWriteRange>();
    private Ram paletteRam;

    private Mapper30(UnromSwitchboard swappableRoms, Rom fixedRom, Ram chrRam, List<ReadWriteRange> nameTableRams, Ram paletteRam) {
        this.swappableRoms = swappableRoms;
        this.fixedRom = fixedRom;
        this.chrRam = chrRam;
        this.nameTableRams.addAll(nameTableRams);
        this.paletteRam = paletteRam;
    }

    /**
     * Configure a Mapper30 instance from the passed in cartridge instance.
     * This will wire up the rom switchboard and the fixed rom through the
     * memory map for efficient lookup.
     * @param cartridge The cartridge containing the roms to add to the memory map.
     * @return Fully configured Mapper2 instance.
     */
    public static Mapper30 configure(Cartridge cartridge) {
        Gdx.app.log(Mapper30.class.getSimpleName(), "Configuring cpu and ppu bus for Mapper 30.");

        //Configure PRG roms
        int prgRomCount = cartridge.getPrgRomCount();
        Rom[] prgRoms = cartridge.getPrgRoms();
        Rom[] swappableRoms = new Rom[prgRomCount - 1];
        for(int i = 0; i < prgRomCount - 1; i++) {
            swappableRoms[i] = new Rom(Mapper30.SWAPPABLE_ROM_BASE_ADDRESS, prgRoms[i].getData());
        }
        UnromSwitchboard unromSwitchboard = new UnromSwitchboard(Mapper30.SWAPPABLE_ROM_BASE_ADDRESS, Mapper30.MAPPER_30_ROM_SIZE, swappableRoms);
        FixedRom fixedRom = new FixedRom(unromSwitchboard, Mapper30.FIXED_ROM_BASE_ADDRESS, prgRoms[prgRomCount - 1].getData());

        //Configure CHR ram
        Ram chrRam = new Ram(Mapper30.MAPPER_30_CHR_RAM_BASE_ADDRESS, Mapper30.MAPPER_30_CHR_RAM_SIZE);

        //Configure PPU ram
        List<ReadWriteRange> nameTableRams = new ArrayList<ReadWriteRange>();
        if (cartridge.isIgnoreMirroringMode()) {
            //TODO: Replace this with a ram that can be swapped out by bit 7 of mapper 30's
            //TODO: control register.
            SelectableRam selectableRam = new SelectableRam(Ppu.NAME_TABLE_0_BASE_ADDRESS, Ppu.NAME_TABLE_0_BASE_ADDRESS + 1023, Ppu.NAMETABLE_RAM_SIZE * 2);
            nameTableRams.add(selectableRam);
            unromSwitchboard.setSelectableRam(selectableRam);
        } else {
            switch (cartridge.getMirroringMode()) {
                case Cartridge.MIRRORING_MODE_HORIZONTAL:
                    nameTableRams.add(new Ram(Ppu.NAME_TABLE_0_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE));
                    nameTableRams.add(new Ram(Ppu.NAME_TABLE_2_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE));
                    break;
                case Cartridge.MIRRORING_MODE_VERTICAL:
                    //We configure vertical mirroring ram as one contiguous chunk to support
                    //legacy state.sav files prior to supporting additional mirroring modes.
                    nameTableRams.add(new Ram(Ppu.NAME_TABLE_0_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE * 2));
                    break;
            }
        }
        Ram paletteRam = new Ram(Ppu.BG_PALETTE_BASE_ADDRESS, Ppu.PALETTE_RAM_SIZE);

        return new Mapper30(unromSwitchboard, fixedRom, chrRam, nameTableRams, paletteRam);
    }

    @Override
    public List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType) {
        ArrayList<ReadWriteRange> readWriteRanges = new ArrayList<ReadWriteRange>();
        switch (busType) {
            case CPU:
                readWriteRanges.add(swappableRoms);
                readWriteRanges.add(fixedRom);
                break;
            case PPU:
                readWriteRanges.add(chrRam);
                readWriteRanges.addAll(nameTableRams);
                readWriteRanges.add(paletteRam);
                break;
        }
        return readWriteRanges;
    }
}

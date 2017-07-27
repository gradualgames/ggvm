package com.gradualgames.ggvm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 8/21/2016.
 *
 * This class imitates the behavior of mapper 2, UnROM. It contains n
 * number of swappable Roms and one fixed rom, mapped to 0x8000 and
 * 0xc000 base addresses just like the real Mapper 2. Any write performed
 * to it is interpreted as a bankswitch command and it change out which of
 * the swappable roms is currently pointed to by the UnromSwitchboard object.
 *
 * Note that this mapper may not exhaustively support all Mapper 2 functionality.
 */
public class Mapper2 implements ReadWriteRangeProvider {

    private static final int MAPPER_2_ROM_SIZE = 0x4000;
    private static final int MAPPER_2_CHR_RAM_BASE_ADDRESS = 0x0000;
    private static final int MAPPER_2_CHR_RAM_SIZE = 0x2000;
    private static final int SWAPPABLE_ROM_BASE_ADDRESS = 0x8000;
    private static final int FIXED_ROM_BASE_ADDRESS = 0xc000;

    private UnromSwitchboard unromSwitchboard;
    private Rom fixedRom;
    private Ram chrRam;
    private List<ReadWriteRange> nameTableRams = new ArrayList<ReadWriteRange>();
    private Ram paletteRam;

    private Mapper2(UnromSwitchboard unromSwitchboard, Rom fixedRom, Ram chrRam, List<ReadWriteRange> nameTableRams, Ram paletteRam) {
        this.unromSwitchboard = unromSwitchboard;
        this.fixedRom = fixedRom;
        this.chrRam = chrRam;
        this.nameTableRams.addAll(nameTableRams);
        this.paletteRam = paletteRam;
    }

    /**
     * Configure a Mapper2 instance from the passed in cartridge instance.
     * This will wire up the rom switchboard and the fixed rom through the
     * memory map for efficient lookup.
     * @param cartridge The cartridge containing the roms to add to the memory map.
     * @return Fully configured Mapper2 instance.
     */
    public static Mapper2 configure(Cartridge cartridge) {
        Gdx.app.log(Mapper2.class.getSimpleName(), "Configuring cpu and ppu bus for Mapper 2.");

        //Configure PRG roms
        int prgRomCount = cartridge.getPrgRomCount();
        Rom[] prgRoms = cartridge.getPrgRoms();
        Rom[] swappableRoms = new Rom[prgRomCount - 1];
        for(int i = 0; i < prgRomCount - 1; i++) {
            swappableRoms[i] = new Rom(Mapper2.SWAPPABLE_ROM_BASE_ADDRESS, prgRoms[i].getData());
        }
        UnromSwitchboard unromSwitchboard = new UnromSwitchboard(Mapper2.SWAPPABLE_ROM_BASE_ADDRESS, Mapper2.MAPPER_2_ROM_SIZE, swappableRoms);
        FixedRom fixedRom = new FixedRom(unromSwitchboard, Mapper2.FIXED_ROM_BASE_ADDRESS, prgRoms[prgRomCount - 1].getData());

        //Configure CHR ram
        Ram chrRam = new Ram(Mapper2.MAPPER_2_CHR_RAM_BASE_ADDRESS, Mapper2.MAPPER_2_CHR_RAM_SIZE);

        //Configure PPU ram
        List<ReadWriteRange> nameTableRams = new ArrayList<ReadWriteRange>();
        switch(cartridge.getMirroringMode()) {
            case Cartridge.MIRRORING_MODE_HORIZONTAL:
                nameTableRams.add(new Ram(Ppu.NAME_TABLE_0_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE));
                nameTableRams.add(new Ram(Ppu.NAME_TABLE_2_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE));
                break;
            case Cartridge.MIRRORING_MODE_VERTICAL:
                //VerticalMirroringRam was added to support games which write to the
                //mirrored ranges of vram. Previously, all games reliably were writing only
                //to the ranges of the base addresses of both nametables for their respective
                //mirroring modes. This class is backwards compatible with the previous code which
                //was just one contiguous chunk of ram for $2000 and $2400. This class allocates
                //precisely the same amount of bytes as the previous approach, the only difference
                //is it maps itself over the full range of nametable addresses, and mirrors any
                //read or write past $2800 back to $2000 and $2400.
                VerticalMirroringRam ram = new VerticalMirroringRam();
                nameTableRams.add(ram);
                break;
        }
        Ram paletteRam = new Ram(Ppu.BG_PALETTE_BASE_ADDRESS, Ppu.PALETTE_RAM_SIZE);

        return new Mapper2(unromSwitchboard, fixedRom, chrRam, nameTableRams, paletteRam);
    }

    @Override
    public List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType) {
        ArrayList<ReadWriteRange> readWriteRanges = new ArrayList<ReadWriteRange>();
        switch (busType) {
            case CPU:
                readWriteRanges.add(unromSwitchboard);
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

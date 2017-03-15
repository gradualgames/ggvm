package com.gradualgames.ggvm;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 9/11/2016.
 *
 * This is the NROM mapper.
 *
 * Note that this mapper may not exhaustively support all Mapper 0 functionality.
 */
public class Mapper0 implements ReadWriteRangeProvider {

    private static final int MAPPER_0_LOWER_PRG_ROM_BASE_ADDRESS = 0x8000;
    private static final int MAPPER_0_UPPER_PRG_ROM_BASE_ADDRESS = 0xc000;
    private static final int MAPPER_0_CHR_ROM_BASE_ADDRESS = 0x0000;

    private Rom lowerPrgRom;
    private Rom upperPrgRom;
    private Rom chrRom;
    private List<Ram> nametableRams = new ArrayList<Ram>();
    private Ram paletteRam;

    private Mapper0(Rom lowerPrgRom, Rom upperPrgRom, Rom chrRom, List<Ram> nametableRams, Ram paletteRam) {
        this.lowerPrgRom = lowerPrgRom;
        this.upperPrgRom = upperPrgRom;
        this.chrRom = chrRom;
        this.nametableRams.addAll(nametableRams);
        this.paletteRam = paletteRam;
    }

    @Override
    public List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType) {
        List<ReadWriteRange> readWriteRanges = new ArrayList<ReadWriteRange>();
        switch(busType) {
            case CPU:
                if (lowerPrgRom != null) {
                    readWriteRanges.add(lowerPrgRom);
                }
                if (upperPrgRom != null) {
                    readWriteRanges.add(upperPrgRom);
                }
                break;
            case PPU:
                readWriteRanges.add(chrRom);
                readWriteRanges.addAll(nametableRams);
                readWriteRanges.add(paletteRam);
                break;
        }
        return readWriteRanges;
    }

    /**
     * Configure a Mapper0 instance from the passed in cartridge instance.
     * This will wire up both the upper and the lower rom to the bus.
     * @param cartridge The cartridge containing the roms to add to the memory map.
     * @return Fully configured Mappe0 instance.
     */
    public static Mapper0 configure(Cartridge cartridge) {
        Gdx.app.log(Mapper0.class.getSimpleName(), "Configuring cpu and ppu bus for Mapper 0.");

        //Configure PRG roms
        int prgRomCount = cartridge.getPrgRomCount();
        Rom[] prgRoms = cartridge.getPrgRoms();
        Rom lowerRom = null;
        Rom upperRom = null;
        if (0 < prgRomCount) {
            upperRom = new Rom(MAPPER_0_LOWER_PRG_ROM_BASE_ADDRESS, prgRoms[0].getData());
        }
        if (1 < prgRoms.length) {
            lowerRom = new Rom(MAPPER_0_UPPER_PRG_ROM_BASE_ADDRESS, prgRoms[1].getData());
        }

        //Configure CHR roms
        int chrRomCount = cartridge.getChrRomCount();
        Rom[] chrRoms = cartridge.getChrRoms();
        Rom chrRom = null;
        if (0 < chrRomCount) {
            chrRom = new Rom(MAPPER_0_CHR_ROM_BASE_ADDRESS, chrRoms[0].getData());
        }

        //Configure PPU RAM
        List<Ram> nametableRams = new ArrayList<Ram>();
        switch(cartridge.getMirroringMode()) {
            case Cartridge.MIRRORING_MODE_HORIZONTAL:
                nametableRams.add(new Ram(Ppu.NAME_TABLE_0_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE));
                nametableRams.add(new Ram(Ppu.NAME_TABLE_2_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE));
                break;
            case Cartridge.MIRRORING_MODE_VERTICAL:
                //We configure vertical mirroring ram as one contiguous chunk to support
                //legacy state.sav files prior to supporting additional mirroring modes.
                nametableRams.add(new Ram(Ppu.NAME_TABLE_0_BASE_ADDRESS, Ppu.NAMETABLE_RAM_SIZE * 2));
                break;
        }
        Ram paletteRam = new Ram(Ppu.BG_PALETTE_BASE_ADDRESS, Ppu.PALETTE_RAM_SIZE);

        return new Mapper0(lowerRom, upperRom, chrRom, nametableRams, paletteRam);
    }
}

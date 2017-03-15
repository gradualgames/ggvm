package com.gradualgames.ggvm;

/**
 * Created by derek on 8/21/2016.
 *
 * This class represents a full game rom, or cartridge. It is responsible
 * for reading the iNES header, parsing the rom into its constituent prg
 * and chr roms, and configuring a mapper which is responsible for providing
 * components to be installed on the cpu and ppu buses.
 */
public class Cartridge {

    private static final int INES_HEADER_SIZE = 16;
    private static final int PRG_ROM_SIZE = 0x4000;
    private static final int CHR_ROM_SIZE = 0x2000;

    public static final int MIRRORING_MODE_HORIZONTAL = 0;
    public static final int MIRRORING_MODE_VERTICAL = 1;

    private int mapper;
    private int mirroringMode;
    private int prgRomCount;
    private int chrRomCount;

    Rom[] prgRoms;
    Rom[] chrRoms;

    /**
     * Configures a cartridge using predefined header data. This is used when we
     * strip the header from a game ROM to discourage casual hackers.
     */
    public Cartridge(int prgRomCount, int chrRomCount, int mapper, int mirroringMode, byte[] bytes) {
        this.prgRomCount = prgRomCount;
        this.chrRomCount = chrRomCount;
        this.mapper = mapper;
        this.mirroringMode = mirroringMode;

        processData(bytes, INES_HEADER_SIZE);
    }

    /**
     * Loads prgRoms and chrRoms from an array of bytes. Uses the iNES
     * format that defines PRG roms as 16kb in size and CHR roms as 8kb
     * in size. Each mapper can slice and dice these into smaller constituent
     * roms as appropriate.
     */
    public Cartridge(byte[] bytes) {
        //Get ROM counts out of iNES header
        prgRomCount = bytes[4];
        chrRomCount = bytes[5];
        mapper = (bytes[6] >> 4) | (bytes[7] & 0xf0);
        mirroringMode = bytes[6] & 1;

        processData(bytes, INES_HEADER_SIZE);
    }

    /**
     * Process the data of a cartridge starting from a given address,
     * assuming that the cartridge specs have already been provided
     * (prgRomCount, chrRomCount, mapper, etc.)
     */
    private void processData(byte[] bytes, int startAddress) {
        int address = startAddress;
        prgRoms = new Rom[prgRomCount];
        chrRoms = new Rom[chrRomCount];
        //Read all the PRG ROMs
        for(int i = 0; i < prgRomCount; i++) {
            byte[] data = new byte[PRG_ROM_SIZE];
            for (int j = 0; j < PRG_ROM_SIZE; j++) {
                data[j] = bytes[address + j];
            }
            Rom prgRom = new Rom(data);
            prgRoms[i] = prgRom;
            address += PRG_ROM_SIZE;
        }

        //Read all the CHR ROMs
        for(int i = 0; i < chrRomCount; i++) {
            byte[] data = new byte[CHR_ROM_SIZE];
            for (int j = 0; j < CHR_ROM_SIZE; j++) {
                data[j] = bytes[address + j];
            }
            Rom chrRom = new Rom(data);
            chrRoms[i] = chrRom;
            address += CHR_ROM_SIZE;
        }
    }

    public int getMirroringMode() {
        return mirroringMode;
    }

    public int getPrgRomCount() {
        return prgRomCount;
    }

    public int getChrRomCount() {
        return chrRomCount;
    }

    public Rom[] getPrgRoms() {
        return prgRoms;
    }

    public Rom[] getChrRoms() {
        return chrRoms;
    }

    /**
     * Determines which mapper this cartridge is using from the iNES header
     * and then calls the appropriate mapper factory method to create a fully
     * configured mapper for this cartridge.
     */
    public ReadWriteRangeProvider configureMapper() {
        if (mapper == 0) {
            return Mapper0.configure(this);
        } else if (mapper == 2) {
            return Mapper2.configure(this);
        }
        return null;
    }
}

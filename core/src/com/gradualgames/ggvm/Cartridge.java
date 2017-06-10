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

    private boolean ignoreMirroringMode = false;
    private int prgRomCount;
    private int chrRomCount;

    Rom[] prgRoms;
    Rom[] chrRoms;

    /**
     * Configures a cartridge using predefined header data. This is used when we
     * strip the header from a game ROM to discourage casual hackers.
     * @param prgRomCount The number of 16kb PRG-ROMs in the cartridge.
     * @param chrRomCount The number of 8kb CHR-ROMs in the cartridge.
     * @param mapper The mapper #. Will be used to select from supported mappers. See
     *               configureMapper.
     * @param mirroringMode The mirroring mode to use, MIRRORING_MODE_HORIZONTAL or
     *                      MIRRORING_MODE_VERTICAL.
     * @param bytes The actual bytes of the cartridge, including 16 bytes where the iNES
     *              header would have been. With this constructor, that header can be zeroed out.
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
     * @param bytes All bytes of the cartridge including the 16 byte iNES header.
     */
    public Cartridge(byte[] bytes) {
        //Get ROM counts out of iNES header
        prgRomCount = bytes[4];
        chrRomCount = bytes[5];
        mapper = ((bytes[6] & 0xff) >> 4) | ((bytes[7] & 0xff) & 0xf0);
        mirroringMode = bytes[6] & 1;
        ignoreMirroringMode = (bytes[6] & 8) != 0;

        processData(bytes, INES_HEADER_SIZE);
    }

    /**
     * Process the data of a cartridge starting from a given address,
     * assuming that the cartridge specs have already been provided
     * (prgRomCount, chrRomCount, mapper, etc.)
     * @param bytes The bytes of the cartridge.
     * @param startAddress The address at which to begin processing data.
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

    /**
     * @return The mirroring mode, as MIRRORING_MODE_HORIZONTAL or
     * MIRRORING_MODE_VERTICAL.
     */
    public int getMirroringMode() {
        return mirroringMode;
    }

    /**
     * @return Whether to ignore the mirroring mode. This will be true when
     * single screen mirroring is active.
     */
    public boolean isIgnoreMirroringMode() {
        return ignoreMirroringMode;
    }

    /**
     * @return The number of PRG-ROMs in this cartridge.
     */
    public int getPrgRomCount() {
        return prgRomCount;
    }

    /**
     * @return The number of CHR-ROMS in this cartridge.
     */
    public int getChrRomCount() {
        return chrRomCount;
    }

    /**
     * @return The array of PRG-ROMs that were parsed from this cartridge.
     */
    public Rom[] getPrgRoms() {
        return prgRoms;
    }

    /**
     * @return The array of CHR-ROMs that were parsed from this cartridge.
     */
    public Rom[] getChrRoms() {
        return chrRoms;
    }

    /**
     * Determines which mapper this cartridge is using from the iNES header
     * and then calls the appropriate mapper factory method to create a fully
     * configured mapper for this cartridge.
     * @return Fully configured mapper as a ReadWriteRangeProvider.
     */
    public ReadWriteRangeProvider configureMapper() {
        if (mapper == 0) {
            return Mapper0.configure(this);
        } else if (mapper == 2) {
            return Mapper2.configure(this);
        } else if (mapper == 30) {
            return Mapper30.configure(this);
        }
        return null;
    }
}

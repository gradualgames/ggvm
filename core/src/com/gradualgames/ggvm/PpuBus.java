package com.gradualgames.ggvm;

/**
 * Created by derek on 9/4/2016.
 *
 * This is the Ppu bus. In conjunction with a mapper (ReadWriteRangeProvider),
 * can support chr-ram and chr-rom. Typically the mapper will always provide
 * palette ram and nametable ram. Depending on the mapper, chr data will be
 * mapped to rom or ram. See Mapper classes for more information.
 */
public class PpuBus extends Bus {

    public static final int MEMORY_MAP_SIZE = 0x4000;

    public PpuBus(ReadWriteRangeProvider mapper, ReadWriteRangeNop readWriteRangeNop) {
        super(BusType.PPU, MEMORY_MAP_SIZE, readWriteRangeNop);
        add(mapper);
        //Fill all null entries with a no-op
        fillNullEntries(readWriteRangeNop);
    }
}

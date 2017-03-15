package com.gradualgames.ggvm;

/**
 * Created by derek on 9/4/2016.
 *
 * This is the CPU bus. Requires ram, spriteRam, controller, ppu,
 * apu and a mapper, each of which are just ReadWriteRange or
 * ReadWriteRangeProvider objects. Bus objects are configured by
 * the GGVm class on construction.
 */
public class CpuBus extends Bus {

    /**
     * The size of the cpu's 64k memory map, plus an extra page for
     * logging warnings in case any instructions attempt to access a page
     * past the end of the 64k address space. (ReadWriteRangeNop will fill
     * all null entries and will log warnings any time read or written to).
     * This will not replicate the behavior of a real NES, where such behavior
     * will trample on ZP.
     */
    public static final int MEMORY_MAP_SIZE = 65536 + 256;

    public CpuBus(
            Ram ram,
            com.gradualgames.ggvm.Controller controller,
            SpriteRam spriteRam,
            ReadWriteRangeProvider ppu,
            ReadWriteRangeProvider apu,
            ReadWriteRangeProvider mapper,
            ReadWriteRangeNop readWriteRangeNop) {
        super(BusType.CPU, MEMORY_MAP_SIZE, readWriteRangeNop);
        //Map the ram object
        add(ram);
        //Map the controller object
        add(controller);
        //Map sprite ram and wire it back up to this bus
        spriteRam.setCpuBus(this);
        add(spriteRam);
        //Map Ppu registers
        add(ppu);
        //Map Apu registers
        add(apu);
        //Map the mapper!
        add(mapper);
        //Fill all null entries with a no-op
        fillNullEntries(readWriteRangeNop);
    }
}

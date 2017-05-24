package com.gradualgames.ggvm;

import com.badlogic.gdx.Gdx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 9/3/2016.
 *
 * This ReadWriteRange object fills all entries of both the cpu and ppu
 * memory map so that any time something that is not hooked up to rom, ram,
 * or hardware will generate a warning and a cpu status readout.
 */
public class ReadWriteRangeWarning implements ReadWriteRange {

    private Cpu cpu;

    private Bus bus;

    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    @Override
    public byte read(int address) {
        int lowerPrgBank = getLowerPrgBank();
        Gdx.app.log(getClass().getSimpleName() + " on " + bus.getClass().getSimpleName(),
                "Address: " +
                Integer.toHexString(address) +
                " was read? Cpu status: " +
                cpu.getRegistersString() +
                " bank: " + (lowerPrgBank != -1 ? lowerPrgBank : ""));
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        int lowerPrgBank = getLowerPrgBank();
        Gdx.app.log(getClass().getSimpleName() + " on " + bus.getClass().getSimpleName(),
                "Address: " +
                        Integer.toHexString(address) +
                        " was written? Cpu status: " +
                        cpu.getRegistersString() +
                        " bank: " + (lowerPrgBank != -1 ? lowerPrgBank : ""));
    }

    @Override
    public int lower() {
        return 0;
    }

    @Override
    public int upper() {
        return 0xffff;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }

    /**
     * Hard coded helper method for UnROM mappers for now to report which
     * bank is swapped in. In the future we may want to delegate this to the
     * mapper itself somehow.
     * @return The bank that is swapped in, or -1 if no switchboard available
     */
    public int getLowerPrgBank() {
        if (bus.busType == Bus.BusType.CPU && bus.memoryMap[0x8000] instanceof RomSwitchboard) {
            RomSwitchboard romSwitchboard = (RomSwitchboard) bus.memoryMap[0x8000];
            return romSwitchboard.getCurrentRom();
        }
        return -1;
    }
}

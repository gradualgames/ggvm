package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * Implements a Rom switchboard for any range of addresses that a mapper can map
 * to different roms. Right now, this class is suitable for use with Mapper 2 or
 * similar mappers, but would probably require modification or interaction with
 * individual registesr for other mappers which have specific locations on the bus
 * and commands accepted there.
 */
public class RomSwitchboard implements ReadWriteRange {

    private int lower;

    private int upper;

    private int size;

    private int currentRom;

    private Rom[] roms;

    public RomSwitchboard(int lower, int size, Rom[] roms) {
        this.lower = lower;
        this.size = size;
        this.upper = this.lower + this.size - 1;
        this.roms = roms;
        this.currentRom = 0;
    }

    public int getCurrentRom() {
        return currentRom;
    }

    @Override
    public byte read(int address) {
        return roms[currentRom].read(address);
    }

    @Override
    public void write(int address, byte value) {
        currentRom = value;
    }

    @Override
    public int lower() {
        return lower;
    }

    @Override
    public int upper() {
        return upper;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        outputStream.write(currentRom);
    }

    @Override
    public void load(InputStream inputStream) throws IOException {
        currentRom = inputStream.read();
    }
}

package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * Represents a RAM of arbitrary size. Can be used for both the 2k
 * hardwired Ram in the CPU and for CHR-RAM by a mapper, for example,
 * or SRAM.
 */
public class Ram implements ReadWriteRange {

    private int lower;
    private int upper;

    private byte[] ram;

    public Ram(int lower, int size) {
        this.lower = lower;
        this.upper = lower + size - 1;
        //Over-allocate space for this chunk of ram and align its base address
        //to its location on the bus. This allows direct access without having
        //to subtract the lower range every time we do a read or write.
        this.ram = new byte[lower + size];
    }

    @Override
    public byte read(int address) {
        return ram[address];
    }

    @Override
    public void write(int address, byte value) {
        ram[address] = value;
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
        outputStream.write(ram);
    }

    @Override
    public void load(InputStream inputStream) throws IOException {
        inputStream.read(ram);
    }
}

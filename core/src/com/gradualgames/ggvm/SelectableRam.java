package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 6/10/2017.
 *
 * This is a RAM which can select where it is currently being written to or
 * read from via an offset property. This is intended to be used with care for
 * situations such as selectable nametable RAM with Mapper 30 for instance.
 */
public class SelectableRam implements ReadWriteRange {

    private int lower;
    private int upper;
    private int offset;

    private byte[] ram;

    public SelectableRam(int lower, int upper, int size) {
        this.lower = lower;
        this.upper = upper;
        //Over-allocate space for this chunk of ram and align its base address
        //to its location on the bus. This allows direct access without having
        //to subtract the lower range every time we do a read or write.
        this.ram = new byte[lower + size];
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public byte read(int address) {
        return ram[address + offset];
    }

    @Override
    public void write(int address, byte value) {
        ram[address + offset] = value;
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
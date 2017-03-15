package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 9/18/2016.
 *
 * Replaces a location on the cpu bus to notify listeners that this location
 * has been read from or written to. Forwards all read and write calls to
 * what it is replacing on the Cpu bus.
 */
public class BusEventGenerator implements ReadWriteRange {

    int lower;
    int upper;
    ReadWriteRange readWriteRange;
    BusListener busListener;

    public BusEventGenerator(int address, int size, ReadWriteRange readWriteRange, BusListener busListener) {
        this.lower = address;
        this.upper = this.lower + size - 1;
        this.readWriteRange = readWriteRange;
        this.busListener = busListener;
    }

    public ReadWriteRange getReadWriteRange() {
        return readWriteRange;
    }

    @Override
    public byte read(int address) {
        busListener.onRead(address);
        return readWriteRange.read(address);
    }

    @Override
    public void write(int address, byte value) {
        busListener.onWrite(address, value);
        readWriteRange.write(address, value);
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

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }
}

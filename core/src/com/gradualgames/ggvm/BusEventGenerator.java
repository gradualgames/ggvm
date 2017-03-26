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

    /**
     * Constructor.
     * @param address Starting address of this BusEventGenerator.
     * @param size How many bytes this BusEventGenerator occupies.
     * @param readWriteRange The ReadWriteRange object that this BusEventGenerator will
     *                       wrap, for later uninstallation if needed.
     * @param busListener The listener to call whenever reads or writes are intercepted by
     *                    this BusEventGenerator.
     */
    public BusEventGenerator(int address, int size, ReadWriteRange readWriteRange, BusListener busListener) {
        this.lower = address;
        this.upper = this.lower + size - 1;
        this.readWriteRange = readWriteRange;
        this.busListener = busListener;
    }

    /**
     * Retrieves the ReadWriteRange object wrapped by this BusEventGenerator.
     * @return The ReadWriteRange object.
     */
    public ReadWriteRange getReadWriteRange() {
        return readWriteRange;
    }

    /**
     * Forwards a read call to the wrapped ReadWriteRange object, first
     * notifying the busListener of the read and where it occurred.
     * @param address The address from which to read.
     * @return The result of calling read on the wrapped ReadWriteRange object.
     */
    @Override
    public byte read(int address) {
        busListener.onRead(address);
        return readWriteRange.read(address);
    }

    /**
     * Forwards a write call to the wrapped ReadWriteRange object, then
     * notifies the bus listener of the write, where it occurred and what
     * the value was.
     * @param address The address at which to write.
     * @param value The byte value to write at this location.
     */
    @Override
    public void write(int address, byte value) {
        readWriteRange.write(address, value);
        busListener.onWrite(address, value);
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
        readWriteRange.save(outputStream);
    }

    @Override
    public void load(InputStream inputStream) throws IOException {
        readWriteRange.load(inputStream);
    }
}

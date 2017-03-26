package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * This is the standard interface for bus communication between the Cpu,
 * Ppu, ram, rom chips and hardware peripherals. It works only with
 * signed java byte objects. Unsigned interpretation of bytes and other
 * types of reads are delegated to the Bus at a higher level.
 */
public interface ReadWriteRange {

    /**
     * Reads a signed byte from this object.
     * @param address The address from which to read.
     * @return A signed byte value.
     */
    byte read(int address);

    /**
     * Writes a signed byte to this object.
     * @param address The address to which to write.
     * @param value A signed byte value to write.
     */
    void write(int address, byte value);

    /**
     * The lower address, inclusive, of this ReadWriteRange object.
     * @return The lower address.
     */
    int lower();

    /**
     * The upper address, inclusive, of this ReadWriteRange object.
     * @return The upper address.
     */
    int upper();

    /**
     * Saves the state of this ReadWriteRange object.
     * @param outputStream An output stream to which to write this ReadWriteRange object's data.
     * @throws IOException
     */
    void save(OutputStream outputStream) throws IOException;

    /**
     * Restores the state of this ReadWriteRange object. Assumes that the stream
     * is being read in the correct order and that the state of this object was properly
     * written when last saved.
     * @param inputStream An input stream from which to restore the state of this ReadWriteRange object.
     * @throws IOException
     */
    void load(InputStream inputStream) throws IOException;
}

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

    byte read(int address);

    void write(int address, byte value);

    int lower();

    int upper();

    void save(OutputStream outputStream) throws IOException;

    void load(InputStream inputStream) throws IOException;
}

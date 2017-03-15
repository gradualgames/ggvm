package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * Represents a ROM of arbitary size, to be configured at a higher
 * level. Can be used for PRG rom or CHR rom. See Mapper classes to
 * see how they are configured.
 */
public class Rom implements ReadWriteRange {

    private byte[] data;
    private int lower;
    private int upper;

    public Rom(byte[] data) {
        this.data = data;
    }

    public Rom(int lower, byte[] data) {
        this(data);
        this.lower = lower;
        this.upper = lower + data.length - 1;
    }

    @Override
    public byte read(int address) {
        return data[address - lower];
    }

    @Override
    public void write(int address, byte value) {

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

    public byte[] getData() {
        return data;
    }
}

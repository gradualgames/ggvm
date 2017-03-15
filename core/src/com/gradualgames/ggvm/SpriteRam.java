package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 9/4/2016.
 *
 * This class implements sprite RAM. It knows about the cpu bus so it can transfer
 * a page of ram into its own internal sprite array. Note this does not exhaustively
 * implement the behavior of sprite ram, and currently only simulates the ram page
 * copy behavior that is the most commonly used. $2003 and $2004 are currently
 * ignored.
 */
public class SpriteRam implements ReadWriteRangeProvider {

    CpuBus cpuBus;

    private byte[] spriteRam = new byte[256];

    private SpriteRam2003 spriteRam2003 = new SpriteRam2003();
    private SpriteRam4014 spriteRam4014 = new SpriteRam4014();

    private List<ReadWriteRange> registers = new ArrayList<ReadWriteRange>();

    public SpriteRam() {
        registers.add(spriteRam2003);
        registers.add(spriteRam4014);
    }

    public void setCpuBus(CpuBus cpuBus) {
        this.cpuBus = cpuBus;
    }

    public int readUnsignedByteAsInt(int address) {
        return spriteRam[address] & 0xff;
    }

    @Override
    public List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType) {
        return registers;
    }

    private class SpriteRam2003 implements ReadWriteRange {

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {

        }

        @Override
        public int lower() {
            return 0x2003;
        }

        @Override
        public int upper() {
            return 0x2003;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {

        }

        @Override
        public void load(InputStream inputStream) throws IOException {

        }
    }

    private class SpriteRam4014 implements ReadWriteRange {

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {
            int pageAddress = value << 8;
            int spriteRamAddress = 0;
            for(int i = pageAddress; i < pageAddress + 256; i++) {
                spriteRam[spriteRamAddress] = cpuBus.readSignedByte(i);
                spriteRamAddress++;
            }
        }

        @Override
        public int lower() {
            return 0x4014;
        }

        @Override
        public int upper() {
            return 0x4014;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {
            outputStream.write(spriteRam);
        }

        @Override
        public void load(InputStream inputStream) throws IOException {
            inputStream.read(spriteRam);
        }
    }
}

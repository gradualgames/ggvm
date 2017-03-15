package com.gradualgames.ggvm;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 9/3/2016.
 *
 * This is the Ppu. It is a collection of registers which emulate the
 * behavior of the ppu at a high level. These registers interact with the
 * PpuBus and ram and rom objects configured there. It also contains constants
 * which are used elsewhere in the application such as when mappers configure
 * themselves.
 *
 * Note: This is not an exhaustive implementation of the ppu, and really only
 * represents bus interaction with rom and ram, and the behavior of the ppu
 * control and status registers at a high level.
 */
public class Ppu implements ReadWriteRangeProvider {

    public static final int NAMETABLE_RAM_SIZE = 0x400;
    public static final int PALETTE_RAM_SIZE = 0x20;

    public static final int BG_PALETTE_BASE_ADDRESS = 0x3f00;
    public static final int SPR_PALETTE_BASE_ADDRESS = 0x3f10;
    public static final int NAME_TABLE_0_BASE_ADDRESS = 0x2000;
    public static final int NAME_TABLE_1_BASE_ADDRESS = 0x2400;
    public static final int NAME_TABLE_2_BASE_ADDRESS = 0x2800;
    public static final int NAME_TABLE_3_BASE_ADDRESS = 0x2c00;
    public static final int ATTRIBUTE_TABLE_0_BASE_ADDRESS = 0x23c0;
    public static final int ATTRIBUTE_TABLE_1_BASE_ADDRESS = 0x27c0;
    public static final int ATTRIBUTE_TABLE_2_BASE_ADDRESS = 0x2bc0;
    public static final int ATTRIBUTE_TABLE_3_BASE_ADDRESS = 0x2fc0;

    /**
     * The ppu bus, needed for reading and writing to/from chr-rom/chr-ram, vram and
     * status/control registers.
     */
    private PpuBus ppuBus;

    /**
     * Ppu control and status registers.
     */
    private Ppu2000 ppu2000 = new Ppu2000();
    private Ppu2001 ppu2001 = new Ppu2001();
    private Ppu2002 ppu2002 = new Ppu2002();
    private Ppu2005 ppu2005 = new Ppu2005();
    private Ppu2006 ppu2006 = new Ppu2006();
    private Ppu2007 ppu2007 = new Ppu2007();

    private List<ReadWriteRange> registers = new ArrayList<ReadWriteRange>();

    public Ppu(PpuBus ppuBus) {
        this.ppuBus = ppuBus;
        registers.add(ppu2000);
        registers.add(ppu2001);
        registers.add(ppu2002);
        registers.add(ppu2005);
        registers.add(ppu2006);
        registers.add(ppu2007);
    }

    public void setInVblank() {
        ppu2002.setInVblank();
    }

    public boolean isNmiEnabled() {
        return ppu2000.isNmiEnabled();
    }

    public int getSpriteSize() { return ppu2000.getSpriteSize(); }

    public int getBackgroundPatternTableAddress() { return ppu2000.getBackgroundPatternTableAddress(); }

    public int getSpritePatternTableAddress() { return ppu2000.getSpritePatternTableAddress(); }

    public boolean isBackgroundVisible() { return ppu2001.isBackgroundVisible(); }

    public boolean isMonochromeDisplayType() { return ppu2001.isMonochromeDisplayType(); }

    public int getScrollX() {
        return ppu2005.x;
    }

    public int getScrollY() {
        return ppu2005.y;
    }

    public int getNametableAddress() {
        return ppu2006.nameTableAddress;
    }

    @Override
    public List<ReadWriteRange> provideReadWriteRanges(Bus.BusType busType) {
        return registers;
    }

    private class Ppu2000 implements ReadWriteRange {

        //bit positions for PPU control register $2000
        public static final int PPU0_EXECUTE_NMI = 1 << 7;
        public static final int PPU0_MASTER_SLAVE = 1 << 6;
        public static final int PPU0_SPRITE_SIZE = 1 << 5;
        public static final int PPU0_BACKGROUND_PATTERN_TABLE_ADDRESS = 1 << 4;
        public static final int PPU0_SPRITE_PATTERN_TABLE_ADDRESS = 1 << 3;
        public static final int PPU0_ADDRESS_INCREMENT = 1 << 2;
        public static final int PPU0_NAMETABLE_ADDRESS1 = 1 << 1;
        public static final int PPU0_NAMETABLE_ADDRESS0 = 1 << 0;

        private int ppu0control;

        public boolean isNmiEnabled() {
            return (ppu0control & PPU0_EXECUTE_NMI) != 0;
        }

        public int getSpriteSize() { return (ppu0control & PPU0_SPRITE_SIZE) != 0 ? 1 : 0; }

        public int getBackgroundPatternTableAddress() { return (ppu0control & PPU0_BACKGROUND_PATTERN_TABLE_ADDRESS) != 0 ? 1 : 0; }

        public int getSpritePatternTableAddress() { return (ppu0control & PPU0_SPRITE_PATTERN_TABLE_ADDRESS) != 0 ? 1 : 0; }

        public int getSelectedNametable() { return ppu0control & 0x3;}

        public boolean isAddressIncrementSet() {
            return (ppu0control & PPU0_ADDRESS_INCREMENT) != 0;
        }

        @Override
        public byte read(int address) {
            return (byte) ppu0control;
        }

        @Override
        public void write(int address, byte value) {
            ppu0control = value & 0xff;
            switch(getSelectedNametable()) {
                case 0:
                    ppu2006.nameTableAddress = NAME_TABLE_0_BASE_ADDRESS;
                    break;
                case 1:
                    ppu2006.nameTableAddress = NAME_TABLE_1_BASE_ADDRESS;
                    break;
                case 2:
                    ppu2006.nameTableAddress = NAME_TABLE_2_BASE_ADDRESS;
                    break;
                case 3:
                    ppu2006.nameTableAddress = NAME_TABLE_3_BASE_ADDRESS;
                    break;
            }
        }

        @Override
        public int lower() {
            return 0x2000;
        }

        @Override
        public int upper() {
            return 0x2000;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {
            outputStream.write(ppu0control);
        }

        @Override
        public void load(InputStream inputStream) throws IOException {
            ppu0control = inputStream.read();
        }
    }

    private class Ppu2001 implements ReadWriteRange {

        private static final int PPU1_FULL_BACKGROUND_COLOR_2 = 1 << 7;
        private static final int PPU1_FULL_BACKGROUND_COLOR_1 = 1 << 6;
        private static final int PPU1_FULL_BACKGROUND_COLOR_0 = 1 << 5;
        private static final int PPU1_COLOR_INTENSITY_2 = 1 << 7;
        private static final int PPU1_COLOR_INTENSITY_1 = 1 << 6;
        private static final int PPU1_COLOR_INTENSITY_0 = 1 <<  5;
        private static final int PPU1_SPRITE_VISIBILITY = 1 << 4;
        private static final int PPU1_BACKGROUND_VISIBILITY = 1 << 3;
        private static final int PPU1_SPRITE_CLIPPING = 1 << 2;
        private static final int PPU1_BACKGROUND_CLIPPING = 1 << 1;
        private static final int PPU1_DISPLAY_TYPE = 1 << 0;

        private int ppu1control;

        public boolean isBackgroundVisible() { return (ppu1control & PPU1_BACKGROUND_VISIBILITY) != 0; }

        public boolean isSpritesVisible() { return (ppu1control & PPU1_SPRITE_VISIBILITY) != 0; }

        public boolean isMonochromeDisplayType() { return (ppu1control & PPU1_DISPLAY_TYPE) != 0; }

        @Override
        public byte read(int address) {
            return (byte) ppu1control;
        }

        @Override
        public void write(int address, byte value) {
            ppu1control = value & 0xff;
        }

        @Override
        public int lower() {
            return 0x2001;
        }

        @Override
        public int upper() {
            return 0x2001;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {
            outputStream.write(ppu1control);
        }

        @Override
        public void load(InputStream inputStream) throws IOException {
            ppu1control = inputStream.read();
        }
    }

    private class Ppu2002 implements ReadWriteRange {

        private static final int PPU_STATUS_IN_VBLANK = 0x80;

        private int status = 0;

        public void setInVblank() {
            this.status |= PPU_STATUS_IN_VBLANK;
        }

        @Override
        public byte read(int address) {
            int result = status;
            status =  status & (~PPU_STATUS_IN_VBLANK);
            ppu2005.writeX = true;
            ppu2006.writeHi = true;
            return (byte) result;
        }

        @Override
        public void write(int address, byte value) {

        }

        @Override
        public int lower() {
            return 0x2002;
        }

        @Override
        public int upper() {
            return 0x2002;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {
            outputStream.write(status);
        }

        @Override
        public void load(InputStream inputStream) throws IOException {
            status = inputStream.read();
        }
    }

    private class Ppu2005 implements ReadWriteRange {

        private int x;
        private int y;
        private boolean writeX = true;

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {
            if (writeX) {
                x = value & 0xff;
            } else {
                y = value & 0xff;
            }
            writeX = !writeX;
        }

        @Override
        public int lower() {
            return 0x2005;
        }

        @Override
        public int upper() {
            return 0x2005;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {
            outputStream.write(x);
            outputStream.write(y);
            outputStream.write(writeX ? 1: 0);
        }

        @Override
        public void load(InputStream inputStream) throws IOException {
            x = inputStream.read();
            y = inputStream.read();
            writeX = inputStream.read() == 1 ? true : false;
        }
    }

    private class Ppu2006 implements ReadWriteRange {

        private int lo;
        private int hi;
        private int vramAddress;
        private int nameTableAddress;
        private boolean writeHi = true;

        public int getVramAddress() {
            return vramAddress;
        }

        public void incVramAddress() {
            if (ppu2000.isAddressIncrementSet()) {
                vramAddress += 32;
            } else {
                vramAddress++;
            }
        }

        @Override
        public byte read(int address) {
            return 0;
        }

        @Override
        public void write(int address, byte value) {
            if (writeHi) {
                hi = value & 0xff;
            } else {
                lo = value & 0xff;
            }
            vramAddress = (hi << 8) | lo;
            writeHi = !writeHi;
            if (vramAddress == Ppu.NAME_TABLE_0_BASE_ADDRESS ||
                vramAddress == Ppu.NAME_TABLE_1_BASE_ADDRESS ||
                vramAddress == Ppu.NAME_TABLE_2_BASE_ADDRESS ||
                vramAddress == Ppu.NAME_TABLE_3_BASE_ADDRESS) {
                nameTableAddress = vramAddress;
            }
            ppu2007.readEnabled = false;
        }

        @Override
        public int lower() {
            return 0x2006;
        }

        @Override
        public int upper() {
            return 0x2006;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.write(lo);
            dataOutputStream.write(hi);
            dataOutputStream.writeBoolean(writeHi);
            dataOutputStream.writeInt(vramAddress);
            dataOutputStream.writeInt(nameTableAddress);
        }

        @Override
        public void load(InputStream inputStream) throws IOException {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            lo = dataInputStream.read();
            hi = dataInputStream.read();
            writeHi = dataInputStream.readBoolean();
            vramAddress = dataInputStream.readInt();
            nameTableAddress = dataInputStream.readInt();
        }
    }

    private class Ppu2007 implements ReadWriteRange {

        private boolean readEnabled = false;

        @Override
        public byte read(int address) {
            if (readEnabled) {
                byte value = ppuBus.readSignedByte(ppu2006.getVramAddress());
                ppu2006.incVramAddress();
                return value;
            } else {
                readEnabled = true;
                return 0;
            }
        }

        @Override
        public void write(int address, byte value) {
            ppuBus.writeIntAsByte(ppu2006.getVramAddress(), value);
            ppu2006.incVramAddress();
        }

        @Override
        public int lower() {
            return 0x2007;
        }

        @Override
        public int upper() {
            return 0x2007;
        }

        @Override
        public void save(OutputStream outputStream) throws IOException {

        }

        @Override
        public void load(InputStream inputStream) throws IOException {

        }
    }
}

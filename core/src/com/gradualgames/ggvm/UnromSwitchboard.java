package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * Implements a Rom switchboard for any range of addresses that a mapper can map
 * to different roms. Right now, this class is suitable for use with Mapper 2 and
 * Mapper 30. It supports 32 PRG roms. It picks up the currently selected CHR-RAM as
 * 0 through 3. Notice that as of this writing, CHR-RAM is write-only. That is, the actual
 * RAM on the cpu bus is only 8kb so will be overwritten each time the CHR-RAM bank is changed,
 * but GGVm picks this up and translates the writes into real texture data, and the
 * ChrRamPatternTableManager listens to this switchboard to determine where to write and
 * read the texture data.
 *
 * It also supports selecting the current nametable, nametable 0 or 1, via a special class
 * called SelectableRam.
 */
public class UnromSwitchboard implements ReadWriteRange {

    private int lower;

    private int upper;

    private int size;

    private int currentNt;

    private int currentChr;

    private int currentRom;

    private SelectableRam selectableRam;

    private Rom[] roms;

    public UnromSwitchboard(int lower, int size, Rom[] roms) {
        this.lower = lower;
        this.size = size;
        this.upper = this.lower + this.size - 1;
        this.roms = roms;
        this.currentRom = 0;
    }

    public void setSelectableRam(SelectableRam selectableRam) {
        this.selectableRam = selectableRam;
    }

    public int getCurrentChr() {
        return currentChr;
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
        int unsignedValue = value & 0xff;
        currentNt = unsignedValue >> 7;
        if (selectableRam != null) {
            selectableRam.setOffset(currentNt == 1 ? 1024: 0);
        }
        currentChr = (unsignedValue & 0x7f) >> 5;
        currentRom = unsignedValue & 0x1f;
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

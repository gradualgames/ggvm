package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 5/27/2017.
 *
 * This virtual register just accepts writes and stores a value for whether GGVm's
 * Sprite 0 Hit status bar feature is enabled.
 */
public class VirtualRegisterStatusBar implements ReadWriteRange {

    public boolean isSprite0HitStatusBarEnabled() {
        return sprite0HitStatusBarEnabled;
    }

    public void setSprite0HitStatusBarEnabled(boolean sprite0HitStatusBarEnabled) {
        this.sprite0HitStatusBarEnabled = sprite0HitStatusBarEnabled;
    }

    private boolean sprite0HitStatusBarEnabled = false;

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        sprite0HitStatusBarEnabled = value != 0;
    }

    @Override
    public int lower() {
        return 0x5500;
    }

    @Override
    public int upper() {
        return 0x5500;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }
}

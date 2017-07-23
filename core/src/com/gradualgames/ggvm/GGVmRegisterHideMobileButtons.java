package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 7/23/2017.
 *
 * A hardware register installed by TouchInputProcessor on mobile devices. A game
 * ROM may write to location 0x5700 to hide or show the dpad, select, start, a and
 * b buttons, according to this bit mask:
 *         |-----dpad
 *         ||----select button
 *         |||---start button
 *         ||||--b button
 *         |||||-a button
 *     %xxx00000
 * bit: 76543210
 *
 * A bit set to '1' means HIDE. A bit set to '0' means SHOW. So if you do nothing,
 * all the buttons will be showing by default.
 */
public class GGVmRegisterHideMobileButtons implements ReadWriteRange {

    private int hideMobileButtons = 0;

    @Override
    public byte read(int address) {
        return (byte) hideMobileButtons;
    }

    @Override
    public void write(int address, byte value) {
        hideMobileButtons = value;
    }

    @Override
    public int lower() {
        return 0x5700;
    }

    @Override
    public int upper() {
        return 0x5700;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        outputStream.write(hideMobileButtons);
    }

    @Override
    public void load(InputStream inputStream) throws IOException {
        hideMobileButtons = inputStream.read();
    }

    public boolean getHideDPad() {
        return (hideMobileButtons & (1 << 4)) != 0;
    }

    public boolean getHideSelect() {
        return (hideMobileButtons & (1 << 3)) != 0;
    }

    public boolean getHideStart() {
        return (hideMobileButtons & (1 << 2)) != 0;
    }

    public boolean getHideB() {
        return (hideMobileButtons & (1 << 1)) != 0;
    }

    public boolean getHideA() {
        return (hideMobileButtons & (1 << 0)) != 0;
    }

    public int getHideMobileButtons() {
        return hideMobileButtons;
    }

}

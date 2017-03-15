package com.gradualgames.ggvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 8/21/2016.
 *
 * Represents the controller. Mapped to the bus at 0x4016 as
 * NES game roms expect. This object is exposed at a high level
 * via the GGVm class, so that a higher level game framework can
 * pass button presses down to this object.
 */
public class Controller implements ReadWriteRange {

    public static final int CONTROLLER_REGISTER_ADDRESS = 0x4016;

    public enum Buttons {
        A(true),
        B(true),
        SELECT(true),
        START(true),
        UP(true),
        DOWN(true),
        LEFT(true),
        RIGHT(true),
        NONE(false);

        private boolean configurable;

        Buttons(boolean configurable) {
            this.configurable = configurable;
        }

        public boolean isConfigurable() {
            return configurable;
        }
    }

    byte[] buttons = new byte[8];

    byte lastWrittenValue = 0;

    byte buttonIndex = 0;

    @Override
    public byte read(int address) {
        return buttons[buttonIndex++];
    }

    @Override
    public void write(int address, byte value) {
        if (value == 0 && lastWrittenValue == 1) {
            buttonIndex = 0;
        }
        lastWrittenValue = value;
    }

    @Override
    public int lower() {
        return CONTROLLER_REGISTER_ADDRESS;
    }

    @Override
    public int upper() {
        return CONTROLLER_REGISTER_ADDRESS;
    }

    public void clear() {
        for(int i = 0; i < buttons.length; i++) {
            buttons[i] = 0;
        }
    }

    public byte[] getButtons() {
        return buttons;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {
        outputStream.write(lastWrittenValue);
        outputStream.write(buttonIndex);
        outputStream.write(buttons);
    }

    @Override
    public void load(InputStream inputStream) throws IOException {
        lastWrittenValue = (byte) inputStream.read();
        buttonIndex = (byte) inputStream.read();
        inputStream.read(buttons);
    }
}

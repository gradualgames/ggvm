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

    /**
     * Represents all 8 buttons of an NES controller, and one more
     * literal to represent no button at all
     */
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

        /**
         * Metadata used by the Menu to determine whether this enum can have an
         * actual keyboard or controller button mapped to it.
         */
        private boolean configurable;

        /**
         * Constructor.
         * @param configurable Whether this button enum is configurable.
         */
        Buttons(boolean configurable) {
            this.configurable = configurable;
        }

        /**
         * @return Whether this button enum is configurable.
         */
        public boolean isConfigurable() {
            return configurable;
        }
    }

    /**
     * Current state for all 8 buttons.
     */
    byte[] buttons = new byte[8];

    /**
     * Last written value to $4014.
     */
    byte lastWrittenValue = 0;

    /**
     * Current index to read from buttons. Auto increments after
     * every read.
     */
    byte buttonIndex = 0;

    /**
     * Reads the state of the current button at buttonIndex then
     * increments buttonIndex. Expects the user will only read 8 times
     * and then write 0 and then 1 to $4014. Other use cases not yet
     * supported.
     * @param address Ignored, this is already mapped to $4014.
     * @return The value of the button at buttonIndex, with a side effect
     * of post-incrementing buttonIndex.
     */
    @Override
    public byte read(int address) {
        return buttons[buttonIndex++];
    }

    /**
     * Resets buttonIndex to 0 when a 1 and then a 0 are written
     * to $4014.
     * @param address Ignored.
     * @param value Expected to be 1 or 0. A 1 followed by a 0 results in
     *              resetting buttonIndex to 0.
     */
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

    /**
     * @return Current array of button state.
     */
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

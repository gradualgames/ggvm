package com.gradualgames.ggvm;

/**
 * Created by derek on 9/4/2016.
 *
 * This is used by mappers with fixed Roms which need to call back into a
 * rom switchboard to provide bankswitching for writes to address ranges that
 * fall within the fixed rom.
 */
public class FixedRom extends Rom {

    private RomSwitchboard romSwitchboard;

    public FixedRom(RomSwitchboard romSwitchboard, int lower, byte[] data) {
        super(lower, data);
        this.romSwitchboard = romSwitchboard;
    }

    @Override
    public void write(int address, byte value) {
        romSwitchboard.write(address, value);
    }
}

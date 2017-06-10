package com.gradualgames.ggvm;

/**
 * Created by derek on 9/4/2016.
 *
 * This is used by mappers with fixed Roms which need to call back into a
 * rom switchboard to provide bankswitching for writes to address ranges that
 * fall within the fixed rom.
 */
public class FixedRom extends Rom {

    private UnromSwitchboard unromSwitchboard;

    public FixedRom(UnromSwitchboard unromSwitchboard, int lower, byte[] data) {
        super(lower, data);
        this.unromSwitchboard = unromSwitchboard;
    }

    @Override
    public void write(int address, byte value) {
        unromSwitchboard.write(address, value);
    }
}

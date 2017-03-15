package com.gradualgames.ggvm;

/**
 * Created by derek on 9/18/2016.
 *
 * An interface to notify the listener that an address has been read from or
 * written to. This is primarily used for soundtrack playback. See game-specific
 * SoundtrackManager such as DushlanSoundtrackManager for an example of how this
 * is used.
 */
public interface BusListener {

    void onRead(int address);

    void onWrite(int address, byte value);

}

package com.gradualgames.ggvm;

import com.gradualgames.manager.soundtrack.GGVmSoundtrackManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 5/27/2017.
 *
 * This ggvm register, located at 0x5600 on the cpu bus, plays a song by
 * forwarding the value written to a GGVmSoundtrackManager.
 */
public class GGVmRegisterPlayMusic implements ReadWriteRange {

    private GGVmSoundtrackManager ggVmSoundtrackManager;

    public GGVmRegisterPlayMusic(GGVmSoundtrackManager ggVmSoundtrackManager) {
        this.ggVmSoundtrackManager = ggVmSoundtrackManager;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        ggVmSoundtrackManager.playSongNum(value & 0xff);
    }

    @Override
    public int lower() {
        return 0x5600;
    }

    @Override
    public int upper() {
        return 0x5600;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }
}

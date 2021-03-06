package com.gradualgames.ggvm;

import com.gradualgames.manager.soundtrack.GGVmSoundtrackManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 5/27/2017.
 *
 * This ggvm register pauses music playback.
 */
public class GGVmRegisterPauseMusic implements ReadWriteRange {

    private GGVmSoundtrackManager ggVmSoundtrackManager;

    public GGVmRegisterPauseMusic(GGVmSoundtrackManager ggVmSoundtrackManager) {
        this.ggVmSoundtrackManager = ggVmSoundtrackManager;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        ggVmSoundtrackManager.pauseMusic();
    }

    @Override
    public int lower() {
        return 0x5602;
    }

    @Override
    public int upper() {
        return 0x5602;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }
}

package com.gradualgames.ggvm;

import com.gradualgames.manager.soundtrack.GGVmSoundtrackManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 5/27/2017.
 *
 * This register resumes music playback.
 */
public class GGVmRegisterResumeMusic implements ReadWriteRange {

    private GGVmSoundtrackManager ggVmSoundtrackManager;

    public GGVmRegisterResumeMusic(GGVmSoundtrackManager ggVmSoundtrackManager) {
        this.ggVmSoundtrackManager = ggVmSoundtrackManager;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void write(int address, byte value) {
        ggVmSoundtrackManager.unpauseMusic();
    }

    @Override
    public int lower() {
        return 0x5603;
    }

    @Override
    public int upper() {
        return 0x5603;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }
}

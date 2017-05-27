package com.gradualgames.ggvm;

import com.gradualgames.manager.soundtrack.GGVmSoundtrackManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by derek on 5/27/2017.
 *
 * This GGVm register located at 0x5604 reads a 1 if music is currently playing
 * or 0 if not.
 */
public class GGVmRegisterMusicStatus implements ReadWriteRange {

    private GGVmSoundtrackManager ggVmSoundtrackManager;

    public GGVmRegisterMusicStatus(GGVmSoundtrackManager ggVmSoundtrackManager) {
        this.ggVmSoundtrackManager = ggVmSoundtrackManager;
    }

    @Override
    public byte read(int address) {
        return ggVmSoundtrackManager.musicIsPlaying() == true ? (byte) 1 : (byte) 0;
    }

    @Override
    public void write(int address, byte value) {

    }

    @Override
    public int lower() {
        return 0x5605;
    }

    @Override
    public int upper() {
        return 0x5605;
    }

    @Override
    public void save(OutputStream outputStream) throws IOException {

    }

    @Override
    public void load(InputStream inputStream) throws IOException {

    }
}

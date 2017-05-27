package com.gradualgames.manager.soundtrack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.gradualgames.ggvm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 5/27/2017.
 *
 * This soundtrack manager can be used straightforwardly by anybody by modifying your
 * game ROM to write to several special registers:
 * $5600 - Plays a song from the song list.
 * $5601 - Plays a sfx from the sfx list.
 * $5602 - Pauses current music.
 * $5603 - Resumes current music.
 * $5604 - Stops all music.
 *
 * There is also one status register available to determine if music is still playing.
 * Since the audio subsystems on some devices on some platforms (Particularly Android's
 * audio subsystem can be unreliable on some devices) it is NOT recommended to use this register.
 * Please avoid it at all costs. You have been warned.
 * $5605 - Reads 1 if music is playing, 0 if not.
 */
public class GGVmSoundtrackManager extends SoundtrackManager implements Music.OnCompletionListener {

    private GGVmRegisterPlayMusic ggVmRegisterPlayMusic;
    private GGVmRegisterPlaySfx ggVmRegisterPlaySfx;
    private GGVmRegisterPauseMusic ggVmRegisterPauseMusic;
    private GGVmRegisterResumeMusic ggVmRegisterResumeMusic;
    private GGVmRegisterStopMusic ggVmRegisterStopMusic;
    private GGVmRegisterMusicStatus ggVmRegisterMusicStatus;

    private List<SongInfo> songList = new ArrayList<SongInfo>();
    private List<String> sfxList = new ArrayList<String>();

    private SongInfo currentSongInfo;

    public GGVmSoundtrackManager(String title, GGVm ggvm, List<SongInfo> songList, List<String> sfxList) {
        super(title, ggvm);
        this.songList.addAll(songList);
        this.sfxList.addAll(sfxList);
        ggVmRegisterPlayMusic = new GGVmRegisterPlayMusic(this);
        ggVmRegisterPlaySfx = new GGVmRegisterPlaySfx(this);
        ggVmRegisterPauseMusic = new GGVmRegisterPauseMusic(this);
        ggVmRegisterResumeMusic = new GGVmRegisterResumeMusic(this);
        ggVmRegisterStopMusic = new GGVmRegisterStopMusic(this);
        ggVmRegisterMusicStatus = new GGVmRegisterMusicStatus(this);
        ggvm.installReadWriteRange(ggVmRegisterPlayMusic);
        ggvm.installReadWriteRange(ggVmRegisterPlaySfx);
        ggvm.installReadWriteRange(ggVmRegisterPauseMusic);
        ggvm.installReadWriteRange(ggVmRegisterResumeMusic);
        ggvm.installReadWriteRange(ggVmRegisterStopMusic);
        ggvm.installReadWriteRange(ggVmRegisterMusicStatus);
    }

    public boolean musicIsPlaying() {
        return currentSong != null ? currentSong.isPlaying() : false;
    }

    public void playSongNum(int songNum) {
        if (songNum >= 0 && songNum < songList.size()) {
            currentSongInfo = songList.get(songNum);
            if (currentSongInfo.introPath.length() > 0 ) {
                playSong(currentSongInfo.introPath, false);
                if (currentSong != null) {
                    currentSong.setOnCompletionListener(this);
                }
            } else {
                playSong(currentSongInfo.path, currentSongInfo.looping);
            }
        } else {
            Gdx.app.log(getClass().getSimpleName(), "Tried to play unknown song num: " + songNum);
        }
    }

    public void playSfxNum(int sfxNum) {
        if (sfxNum >= 0 && sfxNum < sfxList.size()) {
            playSfx(sfxList.get(sfxNum));
        } else {
            Gdx.app.log(getClass().getSimpleName(), "Tried to play unknown sfx num: " + sfxNum);
        }
    }

    @Override
    protected void handleOnRead(int address) {

    }

    @Override
    protected void handleOnWrite(int address, byte value) {

    }

    @Override
    public void onCompletion(Music music) {
        if (currentSongInfo.path.length() > 0) {
            playSong(currentSongInfo.path, currentSongInfo.looping);
        }
    }
}

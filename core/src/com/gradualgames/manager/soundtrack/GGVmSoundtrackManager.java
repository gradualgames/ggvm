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
 */
public class GGVmSoundtrackManager extends SoundtrackManager implements Music.OnCompletionListener {

    private GGVmRegisterPlayMusic ggVmRegisterPlayMusic;
    private GGVmRegisterPlaySfx ggVmRegisterPlaySfx;
    private GGVmRegisterPauseMusic ggVmRegisterPauseMusic;
    private GGVmRegisterResumeMusic ggVmRegisterResumeMusic;
    private GGVmRegisterStopMusic ggVmRegisterStopMusic;

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
        ggvm.installReadWriteRange(ggVmRegisterPlayMusic);
        ggvm.installReadWriteRange(ggVmRegisterPlaySfx);
        ggvm.installReadWriteRange(ggVmRegisterPauseMusic);
        ggvm.installReadWriteRange(ggVmRegisterResumeMusic);
        ggvm.installReadWriteRange(ggVmRegisterStopMusic);
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

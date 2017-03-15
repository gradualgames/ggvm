package com.gradualgames.manager.soundtrack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.BusListener;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by derek on 10/1/2016.
 *
 * Handles the soundtrack of a specific game by listening to when the game
 * tries to play a song or a sound effect. See game specific extensions of
 * this class for an example for how this is done. Depending on the game,
 * various scenarios and behaviors can be supported. Since sound playback
 * in GGVm does not emulate the ppu, sufficiently advanced behavior may not
 * be easily supportable.
 *
 * Note that once a soundtrack is fully implemented for a game, it is possible
 * and recommended to totally remove the implementation of the game's sound
 * engine except for rts from each routine. It is also recommended to remove all
 * of the game's sound data. This way, the rom that is packaged with GGVm,
 * in conjunction with configuring from the GameModule and removing the iNES
 * header, will confound a casual hacker. If anybody fills in the iNES header,
 * they will find that the game has no sound and they wasted their time. They will
 * have a lot of work on their hands if they want to put in their own sound engine
 * to your rom and transcribe all your music by ear!!!
 */
public abstract class SoundtrackManager implements BusListener {

    protected String title;
    protected GGVm ggvm;
    protected Music currentSong;
    protected String currentSongFileName = "";

    protected Map<String, Music> songs = new HashMap<String, Music>();
    protected Map<String, Sound> sfx = new HashMap<String, Sound>();

    public SoundtrackManager(String title, GGVm ggvm) {
        this.title = title;
        this.ggvm = ggvm;
        preloadSfx();
    }

    /**
     * Preloads all sfx for the game prior to execution. This ensures sfx
     * will be heard the first time they are invoked.
     */
    private void preloadSfx() {
        FileHandle sfxDirectory = Gdx.files.internal(title + "/sfx");
        for (FileHandle fileHandle: sfxDirectory.list()) {
            loadSfx(fileHandle.path());
        }
    }

    /**
     * Plays a song from the assets directory. Stops currently playing song if there is one.
     * @param fileName
     * @param looping
     */
    protected void playSong(final String fileName, final boolean looping) {
        playSong(fileName, looping, 0f);
    }

    /**
     * Plays a song from the assets directory. Stops currently playing song if there is one.
     * Starts playing song from specified position in seconds.
     * @param fileName
     * @param looping
     * @param position
     */
    protected void playSong(final String fileName, final boolean looping, final float position) {
        Gdx.app.log(SoundtrackManager.class.getSimpleName(), "playSong(" + fileName + "," + looping + "," + position + ");");
        stopSongs();
        loadSong(fileName);
        Music music = songs.get(fileName);
        currentSong = music;
        currentSongFileName = fileName;
        currentSong.play();
        currentSong.setPosition(position);
        currentSong.setLooping(looping);
    }

    /**
     * Plays a song without stopping any currently playing songs. This should only be used
     * when we know the current song is about to end, for seamless transitions between
     * introductions and looping portions of songs.
     * @param fileName
     * @param looping
     */
    protected void playSongNoStop(final String fileName, final boolean looping) {
        loadSong(fileName);
        Music music = songs.get(fileName);
        currentSong = music;
        currentSongFileName = fileName;
        currentSong.setLooping(looping);
        currentSong.play();
    }

    /**
     * Stops currently playing music
     */
    public void stopMusic() {
        stopSongs();
    }

    /**
     * Stops currently playing sounds.
     */
    public void stopSound() {
        stopSongs();
        stopSfx();
    }

    /**
     * Plays a sound effect from the assets directory.
     * @param fileName
     */
    protected void playSfx(final String fileName) {
        Gdx.app.log(SoundtrackManager.class.getSimpleName(), "playSfx(" + fileName + ");");
        loadSfx(fileName);
        sfx.get(fileName).play();
    }

    /**
     * Plays a sound effect from the assets directory. Stops any
     * currently playing songs.
     * @param fileName
     */
    protected void playSfxStoppingMusic(final String fileName) {
        stopSongs();
        loadSfx(fileName);
        sfx.get(fileName).play();
    }

    /**
     * Handles onRead callbacks from the ggvm thread.
     * Actual handling of these callbacks is posted back
     * to the application thread to avoid causing problems
     * with LibGDX.
     * @param address
     */
    @Override
    public void onRead(int address) {
        handleOnRead(address);
    }

    @Override
    public void onWrite(int address, byte value) {
        handleOnWrite(address, value);
    }

    public void save(OutputStream outputStream) throws IOException {
        outputStream.write(currentSongFileName.length());
        outputStream.write(currentSongFileName.getBytes());
        outputStream.write(currentSong != null && currentSong.isLooping() ? 1 : 0);
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeFloat(currentSong != null ? currentSong.getPosition() : 0f);
    }

    public void load(InputStream inputStream) throws IOException {
        byte[] currentSongFileNameBytes = new byte[inputStream.read()];
        inputStream.read(currentSongFileNameBytes);
        currentSongFileName = new String(currentSongFileNameBytes);
        boolean isLooping = inputStream.read() == 1 ? true : false;
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        if (!currentSongFileName.equals("")) {
            playSong(currentSongFileName, isLooping, dataInputStream.readFloat());
        }
    }

    protected abstract void handleOnRead(int address);

    protected abstract void handleOnWrite(int address, byte value);

    /**
     * Loads a song and associates it with its path.
     * @param fileName
     */
    private void loadSong(final String fileName) {
        if (!songs.containsKey(fileName)) {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(fileName));
            songs.put(fileName, music);
        }
    }

    /**
     * Loads a sound effect and associates it with its path.
     * @param fileName
     */
    private void loadSfx(final String fileName) {
        if (!sfx.containsKey(fileName)) {
            Sound soundeffect = Gdx.audio.newSound(Gdx.files.internal(fileName));
            sfx.put(fileName, soundeffect);
        }
    }

    /**
     * Pauses the current song if available.
     */
    public void pauseMusic() {
        if (currentSong != null) {
            currentSong.pause();
        }
    }

    /**
     * Unpauses the current song if available.
     */
    public void unpauseMusic() {
        if (currentSong != null) {
            if (currentSong.getPosition() > 0f) {
                currentSong.play();
            }
        }
    }

    /**
     * Stops all songs.
     */
    public void stopSongs() {
        for (Music music: songs.values()) {
            music.stop();
        }
    }

    /**
     * Stops all sfx.
     */
    public void stopSfx() {
        for (Sound sound: sfx.values()) {
            sound.stop();
        }
    }
}

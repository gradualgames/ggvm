package com.gradualgames.manager.soundtrack;

/**
 * Created by derek on 5/27/2017.
 */
public class SongInfo {

    public String introPath = "";
    public String path = "";
    public boolean looping = false;

    public SongInfo(String introPath, String path, boolean looping) {
        this.introPath = introPath;
        this.path = path;
        this.looping = looping;
    }
}

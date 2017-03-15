package com.gradualgames.ggvm.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.gradualgames.application.GGVmApplication;
import com.gradualgames.input.KeyboardInputProcessor;
import com.gradualgames.menu.PCMenu;
import com.gradualgames.module.GameModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class DesktopLauncher {
	public static void main (String[] arg) {
	    //Initialize log file
        try {
            File file = new File("log.txt");
            if (file.exists()) {
                file.delete();
            }
            PrintStream printStream = new PrintStream(file);
            System.setOut(printStream);
            System.setErr(printStream);
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("Could not create log file.");
        }
        //Initialize application
        Graphics.DisplayMode selectedDisplayMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
        if (selectedDisplayMode != null) {
            GameModule gameModule = GameModuleProvider.provideGameModule();
            LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
            config.setFromDisplayMode(selectedDisplayMode);
            config.vSyncEnabled = true;
            if (gameModule.provideIconFileName() != null) {
                config.addIcon(gameModule.provideIconFileName(), Files.FileType.Internal);
            }
            new LwjglApplication(new GGVmApplication(gameModule, PCMenu.class, KeyboardInputProcessor.class), config);
        } else {
            System.out.println("Sorry, this game requires a graphics card that can display 32 bpp!");
        }
	}
}

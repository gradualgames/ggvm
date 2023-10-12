package com.gradualgames.ggvm;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.gradualgames.application.GGVmApplication;
import com.gradualgames.input.KeyboardInputProcessor;
import com.gradualgames.menu.PCMenu;
import com.gradualgames.module.DushlanGameModule;
import com.gradualgames.module.GameModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
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
		Graphics.DisplayMode selectedDisplayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
		if (selectedDisplayMode != null) {
			GameModule gameModule = new DushlanGameModule();
			Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
			config.setWindowedMode(640, 480);
			config.useVsync(true);
			if (gameModule.provideIconFileName() != null) {
				config.setWindowIcon(gameModule.provideIconFileName());
			}
			new Lwjgl3Application(new GGVmApplication(gameModule, PCMenu.class, KeyboardInputProcessor.class), config);
		} else {
			System.out.println("Sorry, this game requires a graphics card that can display 32 bpp!");
		}


//		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
//		config.setForegroundFPS(60);
//		config.setTitle("GGVm");
//		new Lwjgl3Application(new GGVmApplication(), config);
	}
}

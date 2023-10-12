package com.gradualgames.ggvm;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.gradualgames.application.GGVmApplication;
import com.gradualgames.input.TouchInputProcessor;
import com.gradualgames.menu.MobileMenu;
import com.gradualgames.module.DushlanGameModule;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.useGyroscope = false;
		config.useWakelock = false;
		initialize(new GGVmApplication(new DushlanGameModule(), MobileMenu.class, TouchInputProcessor.class), config);

	}
}

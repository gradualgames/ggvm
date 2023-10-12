package com.gradualgames.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
//import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.utils.Array;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.menu.Menu;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by derek on 11/20/2016.
 *
 * This is the keyboard input processor. It contains a series of mappings between button indices
 * defined by Controller.Buttons and actual button codes, key codes, and axis codes that come
 * back from LibGDX. These mappings need to be created by the user using the configuration menu,
 * populated from a persistence file that remembers the mappings, or populated by default mappings
 * supplied by LibGDX for specific controllers, if connected.
 */
public class KeyboardInputProcessor extends InputProcessorBase {

    public enum Axis {
        X,
        Y
    }

    private GGVm ggvm;
    private Menu menu;

    private static final String BUTTON_INDEX_TO_KEY_CODE_PREFERENCES = "buttonIndexToKeyCodePreferences";
    private static final String BUTTON_INDEX_TO_BUTTON_PREFERENCES = "buttonIndexToButtonPreferences";
    private static final String ACTUAL_AXIS_TO_AXIS_CODE_PREFERENCES = "actualAxisToAxisCodePreferences";

    public static boolean isXbox360Controller(String name) {
        if (name.equals(XBOX_360_NAME)) return true;
        if (name.equals(XBOX_360_ALT_NAME)) return true;
        if (name.contains("XBOX 360")) return true;
        return false;
    }

    public static final String XBOX_360_NAME = "Controller (XBOX 360 For Windows)";
    public static final String XBOX_360_ALT_NAME = "XBOX 360 For Windows (Controller)";
    public static final int XBOX_360_A = 0;
    public static final int XBOX_360_B = 1;
    public static final int XBOX_360_X = 2;
    public static final int XBOX_360_Y = 3;
    public static final int XBOX_360_LEFT_SHOULDER = 4;
    public static final int XBOX_360_RIGHT_SHOULDER = 5;
    public static final int XBOX_360_BACK = 6;
    public static final int XBOX_360_START = 7;

    public static final String RETRO_USB_NAME = "Retr";
    public static final int RETRO_USB_A = 1;
    public static final int RETRO_USB_B = 0;
    public static final int RETRO_USB_SELECT = 2;
    public static final int RETRO_USB_START = 3;

    private BiMap<com.gradualgames.ggvm.Controller.Buttons, Integer> buttonIndexToKeyCode = HashBiMap.create();
    private BiMap<com.gradualgames.ggvm.Controller.Buttons, Integer> buttonIndexToButton = HashBiMap.create();
    private BiMap<Axis, Integer> actualAxisToAxisCode = HashBiMap.create();

    protected Map<Integer, Integer> buttonToButtonUpIndex = new HashMap<Integer, Integer>();

    public KeyboardInputProcessor() {
        super();
        buttonToButtonUpIndex.put(com.gradualgames.ggvm.Controller.Buttons.LEFT.ordinal(), com.gradualgames.ggvm.Controller.Buttons.RIGHT.ordinal());
        buttonToButtonUpIndex.put(com.gradualgames.ggvm.Controller.Buttons.RIGHT.ordinal(), com.gradualgames.ggvm.Controller.Buttons.LEFT.ordinal());
        buttonToButtonUpIndex.put(com.gradualgames.ggvm.Controller.Buttons.UP.ordinal(), com.gradualgames.ggvm.Controller.Buttons.DOWN.ordinal());
        buttonToButtonUpIndex.put(com.gradualgames.ggvm.Controller.Buttons.DOWN.ordinal(), com.gradualgames.ggvm.Controller.Buttons.UP.ordinal());
        logControllers();
    }

    @Override
    public void setGGVm(GGVm ggvm) {
        this.ggvm = ggvm;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public void activate() {
        Gdx.input.setInputProcessor(this);
        Controllers.clearListeners();
        Controllers.addListener(this);
    }

    public void saveConfiguration() {
        Preferences buttonIndexToKeyCodePreferences = Gdx.app.getPreferences(BUTTON_INDEX_TO_KEY_CODE_PREFERENCES);
        Preferences buttonIndexToButtonPreferences = Gdx.app.getPreferences(BUTTON_INDEX_TO_BUTTON_PREFERENCES);
        buttonIndexToKeyCodePreferences.clear();
        buttonIndexToButtonPreferences.clear();

        for(com.gradualgames.ggvm.Controller.Buttons button: com.gradualgames.ggvm.Controller.Buttons.values()) {
            if (button.isConfigurable()) {
                if (buttonIndexToKeyCode.containsKey(button)) {
                    buttonIndexToKeyCodePreferences.putInteger(button.name(), buttonIndexToKeyCode.get(button));
                }
                if (buttonIndexToButton.containsKey(button)) {
                    buttonIndexToButtonPreferences.putInteger(button.name(), buttonIndexToButton.get(button));
                }
            }
        }
        buttonIndexToKeyCodePreferences.flush();
        buttonIndexToButtonPreferences.flush();

        Preferences actualAxisToAxisCodePreferences = Gdx.app.getPreferences(ACTUAL_AXIS_TO_AXIS_CODE_PREFERENCES);
        actualAxisToAxisCodePreferences.clear();
        for(Axis axis: Axis.values()) {
            if (actualAxisToAxisCode.containsKey(axis)) {
                actualAxisToAxisCodePreferences.putInteger(axis.name(), actualAxisToAxisCode.get(axis));
            }
        }
        actualAxisToAxisCodePreferences.flush();
    }

    public void loadConfiguration() {
        clear();
        Preferences buttonIndexToKeyCodePreferences = Gdx.app.getPreferences(BUTTON_INDEX_TO_KEY_CODE_PREFERENCES);
        Preferences buttonIndexToButtonPreferences = Gdx.app.getPreferences(BUTTON_INDEX_TO_BUTTON_PREFERENCES);
        for(com.gradualgames.ggvm.Controller.Buttons button: com.gradualgames.ggvm.Controller.Buttons.values()) {
            if (buttonIndexToKeyCodePreferences.contains(button.name())) {
                buttonIndexToKeyCode.put(button, buttonIndexToKeyCodePreferences.getInteger(button.name()));
            }
            if (buttonIndexToButtonPreferences.contains(button.name())) {
                buttonIndexToButton.put(button, buttonIndexToButtonPreferences.getInteger(button.name()));
            }
        }

        Preferences actualAxisToAxisCodePreferences = Gdx.app.getPreferences(ACTUAL_AXIS_TO_AXIS_CODE_PREFERENCES);
        for(Axis axis: Axis.values()) {
            if (actualAxisToAxisCodePreferences.contains(axis.name())) {
                actualAxisToAxisCode.put(axis, actualAxisToAxisCodePreferences.getInteger(axis.name()));
            }
        }

        //If all mappings are empty after attempting to load config, attempt auto config
        if (buttonIndexToKeyCode.isEmpty() &&
            buttonIndexToButton.isEmpty() &&
            actualAxisToAxisCode.isEmpty()) {
            autoConfigure();
        }
    }

    public void clear() {
        buttonIndexToKeyCode.clear();
        buttonIndexToButton.clear();
        actualAxisToAxisCode.clear();
    }

    private void logControllers() {
        Array<Controller> controllers = Controllers.getControllers();
        Gdx.app.log(getClass().getSimpleName(), "****** Controller Names *********");
        for(Controller controller: controllers) {
            Gdx.app.log(getClass().getSimpleName(), controller.getName());
        }
        Gdx.app.log(getClass().getSimpleName(), "********************************");
    }

    public void autoConfigure() {
        Array<Controller> controllers = Controllers.getControllers();
        if (controllers.size > 0) {
            Controller controller = controllers.get(0);
            if (isXbox360Controller(controller.getName())) {
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.A, XBOX_360_A);
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.B, XBOX_360_X);
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.SELECT, XBOX_360_LEFT_SHOULDER);
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.START, XBOX_360_START);
            } else if (controller.getName().equals(RETRO_USB_NAME)) {
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.A, RETRO_USB_A);
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.B, RETRO_USB_B);
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.SELECT, RETRO_USB_SELECT);
                buttonIndexToButton.put(com.gradualgames.ggvm.Controller.Buttons.START, RETRO_USB_START);
                actualAxisToAxisCode.put(Axis.X, 1);
                actualAxisToAxisCode.put(Axis.Y, 0);
            }
        }
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.A, Input.Keys.F);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.B, Input.Keys.D);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.SELECT, Input.Keys.A);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.START, Input.Keys.S);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.UP, Input.Keys.UP);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.RIGHT, Input.Keys.RIGHT);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.DOWN, Input.Keys.DOWN);
        buttonIndexToKeyCode.put(com.gradualgames.ggvm.Controller.Buttons.LEFT, Input.Keys.LEFT);
    }

    public String getNameForKeycode(int keyCode) {
        return Input.Keys.toString(keyCode);
    }

    public String getNameForButton(int button) {
        Array<Controller> controllers = Controllers.getControllers();
        if (controllers.size > 0) {
            Controller controller = controllers.get(0);
            if (KeyboardInputProcessor.isXbox360Controller(controller.getName())) {
                switch(button) {
                    case XBOX_360_A:
                        return "A";
                    case XBOX_360_B:
                        return "B";
                    case XBOX_360_X:
                        return "X";
                    case XBOX_360_Y:
                        return "Y";
                    case XBOX_360_LEFT_SHOULDER:
                        return "LSHLDR";
                    case XBOX_360_RIGHT_SHOULDER:
                        return "RSHLDR";
                    case XBOX_360_BACK:
                        return "BACK";
                    case XBOX_360_START:
                        return "START";
                }
            } else if (controller.getName().equals(RETRO_USB_NAME)) {
                switch(button) {
                    case RETRO_USB_A:
                        return "A";
                    case RETRO_USB_B:
                        return "B";
                    case RETRO_USB_SELECT:
                        return "SELECT";
                    case RETRO_USB_START:
                        return "START";
                }
            }
        }
        return Integer.toString(button);
    }

    public BiMap<com.gradualgames.ggvm.Controller.Buttons, Integer> getButtonIndexToKeyCode() {
        return buttonIndexToKeyCode;
    }

    public BiMap<com.gradualgames.ggvm.Controller.Buttons, Integer> getButtonIndexToButton() {
        return buttonIndexToButton;
    }

    public BiMap<Axis, Integer> getActualAxisToAxisCode() {
        return actualAxisToAxisCode;
    }

    private void stopGGVmAndActivateMenu() {
        ggvm.stop();
        menu.activate();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (buttonIndexToKeyCode.containsValue(keycode)) {
            int buttonIndex = buttonIndexToKeyCode.inverse().get(keycode).ordinal();
            ggvm.setButtonState(buttonIndex, true);
            if (buttonToButtonUpIndex.containsKey(buttonIndex)) {
                ggvm.setButtonState(buttonToButtonUpIndex.get(buttonIndex), false);
            }
        }
        if (keycode == Input.Keys.ESCAPE) {
            stopGGVmAndActivateMenu();
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (buttonIndexToKeyCode.containsValue(keycode)) {
            ggvm.setButtonState(buttonIndexToKeyCode.inverse().get(keycode).ordinal(), false);
        }
        return false;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (buttonIndexToButton.containsValue(buttonCode)) {
            ggvm.setButtonState(buttonIndexToButton.inverse().get(buttonCode).ordinal(), true);
        }
        if (KeyboardInputProcessor.isXbox360Controller(controller.getName()) && buttonCode == XBOX_360_BACK) {
            stopGGVmAndActivateMenu();
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (buttonIndexToButton.containsValue(buttonCode)) {
            ggvm.setButtonState(buttonIndexToButton.inverse().get(buttonCode).ordinal(), false);
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (actualAxisToAxisCode.containsValue(axisCode)) {
            Axis actualAxis = actualAxisToAxisCode.inverse().get(axisCode);
            switch(actualAxis) {
                case X:
                    if (value >= .5) {
                        ggvm.setLeftButtonState(false);
                        ggvm.setRightButtonState(true);
                    } else if (value <= -.5 ) {
                        ggvm.setLeftButtonState(true);
                        ggvm.setRightButtonState(false);
                    } else {
                        ggvm.setLeftButtonState(false);
                        ggvm.setRightButtonState(false);
                    }
                    break;
                case Y:
                    if (value > .5) {
                        ggvm.setDownButtonState(true);
                        ggvm.setUpButtonState(false);
                    } else if (value < -.5) {
                        ggvm.setDownButtonState(false);
                        ggvm.setUpButtonState(true);
                    } else {
                        ggvm.setDownButtonState(false);
                        ggvm.setUpButtonState(false);
                    }
                    break;
            }
        }
        return false;
    }

//    @Override
//    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
//        //Only use d-pad if axes are not configured
//        if (actualAxisToAxisCode.isEmpty()) {
//            switch (value) {
//                case center:
//                    ggvm.setUpButtonState(false);
//                    ggvm.setRightButtonState(false);
//                    ggvm.setDownButtonState(false);
//                    ggvm.setLeftButtonState(false);
//                    break;
//                case north:
//                    ggvm.setUpButtonState(true);
//                    ggvm.setRightButtonState(false);
//                    ggvm.setDownButtonState(false);
//                    ggvm.setLeftButtonState(false);
//                    break;
//                case northEast:
//                    ggvm.setUpButtonState(true);
//                    ggvm.setRightButtonState(true);
//                    ggvm.setDownButtonState(false);
//                    ggvm.setLeftButtonState(false);
//                    break;
//                case east:
//                    ggvm.setUpButtonState(false);
//                    ggvm.setRightButtonState(true);
//                    ggvm.setDownButtonState(false);
//                    ggvm.setLeftButtonState(false);
//                    break;
//                case southEast:
//                    ggvm.setUpButtonState(false);
//                    ggvm.setRightButtonState(true);
//                    ggvm.setDownButtonState(true);
//                    ggvm.setLeftButtonState(false);
//                    break;
//                case south:
//                    ggvm.setUpButtonState(false);
//                    ggvm.setRightButtonState(false);
//                    ggvm.setDownButtonState(true);
//                    ggvm.setLeftButtonState(false);
//                    break;
//                case southWest:
//                    ggvm.setUpButtonState(false);
//                    ggvm.setRightButtonState(false);
//                    ggvm.setDownButtonState(true);
//                    ggvm.setLeftButtonState(true);
//                    break;
//                case west:
//                    ggvm.setUpButtonState(false);
//                    ggvm.setRightButtonState(false);
//                    ggvm.setDownButtonState(false);
//                    ggvm.setLeftButtonState(true);
//                    break;
//                case northWest:
//                    ggvm.setUpButtonState(true);
//                    ggvm.setRightButtonState(false);
//                    ggvm.setDownButtonState(false);
//                    ggvm.setLeftButtonState(true);
//                    break;
//            }
//        }
//        return false;
//    }
}

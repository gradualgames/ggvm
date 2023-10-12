package com.gradualgames.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
//import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.input.InputProcessorBase;
import com.gradualgames.input.KeyboardInputProcessor;
import com.gradualgames.manager.soundtrack.SoundtrackManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 11/20/2016.
 *
 * This is the menu manager for desktop builds of GGVm. It allows
 * the user to configure the controller and change fullscreen/windowed mode.
 */
public class PCMenu extends Menu {

    private FitViewport viewPort;
    private Camera camera;
    private BitmapFont bitmapFont;
    private Menu topLevelMenu;
    private Menu promptInputMenu;
    private Menu menu;
    private List<MenuOption> menuOptions;
    private MenuOption currentMenuOption;
    private ReturnToTitleOption returnToTitleOption;
    private KeyboardInputProcessor inputProcessor;
    private GGVm ggvm;
    private SoundtrackManager soundtrackManager;

    @Override
    public void setDependencies(String fontFileName, String fontBitmapFileName, GGVm ggvm, InputProcessorBase inputProcessor, SoundtrackManager soundtrackManager) {
        FileHandle fontFileHandle = Gdx.files.internal(fontFileName);
        FileHandle fontBitmapFileHandle = Gdx.files.internal(fontBitmapFileName);
        bitmapFont = new BitmapFont(fontFileHandle, fontBitmapFileHandle, false);
        topLevelMenu = new TopLevelMenu();
        promptInputMenu = new PromptInputMenu();
        menu = topLevelMenu;
        this.ggvm = ggvm;
        this.inputProcessor = (KeyboardInputProcessor) inputProcessor;
        this.soundtrackManager = soundtrackManager;
    }

    @Override
    public void activate() {
        super.activate();
        soundtrackManager.pauseMusic();
        this.menu = topLevelMenu;
        this.topLevelMenu.activate();
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewPort = new FitViewport(256, 240, camera);
        viewPort.apply();
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        viewPort.update(width, height);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        if (!ggvm.isAlive()) {
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            menu.render(spriteBatch);
            spriteBatch.end();
        }
    }

    private class TopLevelMenu extends Menu {

        private static final int CURSOR_WIDTH = 10;
        private static final int CURSOR_HEIGHT = 10;
        private static final int CURSOR_RADIUS = 4;
        private static final int CURSOR_MENU_OPTION_X_OFFSET = -10;
        private static final int CURSOR_MENU_OPTION_Y_OFFSET = -11;
        private static final int MENU_WIDTH = 250;
        private static final int MENU_HEIGHT = 170;
        private static final int MENU_BOTTOM_LEFT_X = (256 - MENU_WIDTH) / 2;
        private static final int MENU_BOTTOM_LEFT_Y = (240 - MENU_HEIGHT) / 2;
        private static final int MENU_OPTION_X_OFFSET = 16;
        private static final int MENU_OPTION_Y_OFFSET = -34;
        private static final int MENU_OPTION_ROW_HEIGHT = 12;

        private Texture cursorTexture;
        private Texture menuBackgroundTexture;

        public TopLevelMenu() {
            Pixmap pixmap = new Pixmap(CURSOR_WIDTH, CURSOR_HEIGHT, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(CURSOR_RADIUS, CURSOR_RADIUS, CURSOR_RADIUS);
            cursorTexture = new Texture(pixmap);

            pixmap = new Pixmap(MENU_WIDTH, MENU_HEIGHT, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fillRectangle(0, 0, MENU_WIDTH, MENU_HEIGHT);
            pixmap.setColor(Color.BLACK);
            pixmap.fillRectangle(2, 2, MENU_WIDTH - 4, MENU_HEIGHT - 4);
            menuBackgroundTexture = new Texture(pixmap);

            menuOptions = new ArrayList<MenuOption>();

            List<com.gradualgames.ggvm.Controller.Buttons> configurableButtons = new ArrayList<com.gradualgames.ggvm.Controller.Buttons>();
            for(int i = 0; i < com.gradualgames.ggvm.Controller.Buttons.values().length; i++) {
                com.gradualgames.ggvm.Controller.Buttons button = com.gradualgames.ggvm.Controller.Buttons.values()[i];
                if (button.isConfigurable()) {
                    configurableButtons.add(button);
                }
            }

            int position = 0;

            MenuOption menuOption =
                    new FullscreenWindowedToggleMenuOption(
                            MENU_BOTTOM_LEFT_X + MENU_OPTION_X_OFFSET,
                            MENU_BOTTOM_LEFT_Y + 128 - MENU_OPTION_Y_OFFSET - MENU_OPTION_ROW_HEIGHT * position++,
                            Gdx.graphics.isFullscreen() ? "TO WINDOWED" : "TO FULLSCREEN");
            menuOptions.add(menuOption);

            menuOption =
                    new ReturnToTitleOption(
                            MENU_BOTTOM_LEFT_X + MENU_OPTION_X_OFFSET,
                            MENU_BOTTOM_LEFT_Y + 128 - MENU_OPTION_Y_OFFSET - MENU_OPTION_ROW_HEIGHT * position++);
            menuOptions.add(menuOption);
            returnToTitleOption = (ReturnToTitleOption) menuOption;

            menuOption =
                    new SaveStateAndExitMenuOption(
                            MENU_BOTTOM_LEFT_X + MENU_OPTION_X_OFFSET,
                            MENU_BOTTOM_LEFT_Y + 128 - MENU_OPTION_Y_OFFSET - MENU_OPTION_ROW_HEIGHT * position++,
                            "SAVE GAME AND EXIT");
            menuOptions.add(menuOption);

            menuOption =
                    new ExitMenuOption(
                            MENU_BOTTOM_LEFT_X + MENU_OPTION_X_OFFSET,
                            MENU_BOTTOM_LEFT_Y + 128 - MENU_OPTION_Y_OFFSET - MENU_OPTION_ROW_HEIGHT * position++,
                            "EXIT MENU");
            menuOptions.add(menuOption);

            menuOption =
                    new DefaultsMenuOption(
                            MENU_BOTTOM_LEFT_X + MENU_OPTION_X_OFFSET,
                            MENU_BOTTOM_LEFT_Y + 128 - MENU_OPTION_Y_OFFSET - MENU_OPTION_ROW_HEIGHT * position++,
                            "DEFAULTS");
            menuOptions.add(menuOption);

            for(int i = 0; i < configurableButtons.size(); i++) {
                com.gradualgames.ggvm.Controller.Buttons button = configurableButtons.get(i);
                menuOption =
                    new ButtonMenuOption(
                            MENU_BOTTOM_LEFT_X + MENU_OPTION_X_OFFSET,
                            MENU_BOTTOM_LEFT_Y + 128 - MENU_OPTION_Y_OFFSET - MENU_OPTION_ROW_HEIGHT * position++,
                            button,
                            button.name());
                menuOptions.add(menuOption);
            }

            for(int i = 0; i < menuOptions.size(); i++) {
                menuOption = menuOptions.get(i);
                if (i - 1 >= 0) {
                    menuOption.previousMenuOption = menuOptions.get(i - 1);
                }
                if (i + 1 < menuOptions.size()) {
                    menuOption.nextMenuOption = menuOptions.get(i + 1);
                }
            }

            currentMenuOption = menuOptions.get(0);
        }

        private void cursorUp() {
            if (currentMenuOption.previousMenuOption != null) {
                currentMenuOption = currentMenuOption.previousMenuOption;
                returnToTitleOption.resetVisited();
            }
        }

        private void cursorDown() {
            if (currentMenuOption.nextMenuOption != null) {
                currentMenuOption = currentMenuOption.nextMenuOption;
                returnToTitleOption.resetVisited();
            }
        }

        private void menuAction() {
            currentMenuOption.action();
        }

        private void dismissMenu() {
            soundtrackManager.unpauseMusic();
            inputProcessor.activate();
            inputProcessor.saveConfiguration();
            ggvm.start();
        }

        @Override
        public void render(SpriteBatch spriteBatch) {
            spriteBatch.disableBlending();
            spriteBatch.setShader(null);
            spriteBatch.draw(menuBackgroundTexture, MENU_BOTTOM_LEFT_X, MENU_BOTTOM_LEFT_Y);
            bitmapFont.setColor(Color.WHITE);
            for(MenuOption menuOption: menuOptions) {
                bitmapFont.draw(spriteBatch, menuOption.text, menuOption.x, menuOption.y);
                String key = "";
                String button = "";
                String axisOrDpad = "";
                if (inputProcessor.getButtonIndexToKeyCode().containsKey(menuOption.buttonIndex)) {
                    key = "KEY: " + inputProcessor.getNameForKeycode(
                            inputProcessor.getButtonIndexToKeyCode().get(menuOption.buttonIndex));
                }
                if (inputProcessor.getButtonIndexToButton().containsKey(menuOption.buttonIndex)) {
                    button = " BUTTON: " + inputProcessor.getNameForButton(
                            inputProcessor.getButtonIndexToButton().get(menuOption.buttonIndex));
                }

                if (menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.LEFT ||
                    menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.RIGHT) {
                    if (inputProcessor.getActualAxisToAxisCode().containsKey(KeyboardInputProcessor.Axis.X)) {
                        axisOrDpad = " AXIS " + inputProcessor.getActualAxisToAxisCode().get(KeyboardInputProcessor.Axis.X).toString();
                    }
                 }
                if (menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.UP ||
                    menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.DOWN) {
                    if (inputProcessor.getActualAxisToAxisCode().containsKey(KeyboardInputProcessor.Axis.Y)) {
                        axisOrDpad = " AXIS " + inputProcessor.getActualAxisToAxisCode().get(KeyboardInputProcessor.Axis.Y).toString();
                    }
                }
                if (menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.LEFT ||
                    menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.RIGHT ||
                    menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.UP ||
                    menuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.DOWN) {
                    if (inputProcessor.getActualAxisToAxisCode().isEmpty()) {
                        axisOrDpad = " DPAD";
                    }
                }
                int offset = 64;
                if (key.length() > 0) {
                    bitmapFont.draw(spriteBatch, key, menuOption.x + offset, menuOption.y);
                    offset += 64;
                }
                if (button.length() > 0) {
                    bitmapFont.draw(spriteBatch, button, menuOption.x + offset, menuOption.y);
                    offset += 64;
                }
                if (axisOrDpad.length() > 0) {
                    bitmapFont.draw(spriteBatch, axisOrDpad, menuOption.x + offset, menuOption.y);
                }
            }
            if (currentMenuOption != null) {
                spriteBatch.draw(cursorTexture, currentMenuOption.x + CURSOR_MENU_OPTION_X_OFFSET, currentMenuOption.y + CURSOR_MENU_OPTION_Y_OFFSET);
            }
        }

        @Override
        public boolean keyDown(int keycode) {
            if (currentMenuOption != null) {
                switch(keycode) {
                    case Input.Keys.DOWN:
                        cursorDown();
                        return true;
                    case Input.Keys.UP:
                        cursorUp();
                        return true;
                    case Input.Keys.ENTER:
                        menuAction();
                        return true;
                    case Input.Keys.ESCAPE:
                        dismissMenu();
                        return true;
                    default:
                        returnToTitleOption.resetVisited();
                        break;
                }
            }
            return false;
        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            if (currentMenuOption != null && KeyboardInputProcessor.isXbox360Controller(controller.getName())) {
                switch(buttonCode) {
                    case KeyboardInputProcessor.XBOX_360_A:
                        menuAction();
                        return true;
                    case KeyboardInputProcessor.XBOX_360_BACK:
                        dismissMenu();
                        return true;
                }
            }
            if (currentMenuOption == returnToTitleOption && KeyboardInputProcessor.isXbox360Controller(controller.getName())) {
                if (buttonCode != KeyboardInputProcessor.XBOX_360_A) {
                    returnToTitleOption.resetVisited();
                }
            }
            return false;
        }

//        @Override
//        public boolean povMoved(Controller controller, int povCode, PovDirection value) {
//            switch(value) {
//                case north:
//                    cursorUp();
//                    break;
//                case south:
//                    cursorDown();
//                    break;
//            }
//            return false;
//        }
    }

    private class PromptMenu extends Menu {

        protected int menuWidth = 230;
        protected int menuHeight = 64;
        protected int menuBottomLeftX = (256 - 230) / 2;
        protected int menuBottomLeftY = (240 - 64) / 2;

        private String text;

        private Texture menuBackgroundTexture;

        public PromptMenu(String text) {
            this.text = text;
        }

        protected void initialize() {
            Pixmap pixmap = new Pixmap(menuWidth, menuHeight, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.BLACK);
            pixmap.fillRectangle(0, 0, menuWidth, menuHeight);
            pixmap.setColor(Color.WHITE);
            pixmap.drawRectangle(0, 0, menuWidth, menuHeight);
            menuBackgroundTexture = new Texture(pixmap);
        }

        @Override
        public void render(SpriteBatch spriteBatch) {
            spriteBatch.disableBlending();
            spriteBatch.setShader(null);
            spriteBatch.draw(menuBackgroundTexture, menuBottomLeftX, menuBottomLeftY);
            bitmapFont.setColor(Color.WHITE);
            bitmapFont.draw(spriteBatch,
                    text,
                    menuBottomLeftX + 16, menuBottomLeftY + menuHeight - 16);
        }
    }

    private class PromptInputMenu extends PromptMenu {

        public PromptInputMenu() {
            super("Press desired key or button for:");
            initialize();
        }

        @Override
        public void render(SpriteBatch spriteBatch) {
            super.render(spriteBatch);
            bitmapFont.draw(spriteBatch,
                    currentMenuOption.text,
                    menuBottomLeftX + 16, menuBottomLeftY + menuHeight - 32);
        }

        @Override
        public boolean keyDown(int keycode) {
            Gdx.app.log(getClass().getSimpleName(), "keyDown(" + keycode + ")");
            if (keycode != Input.Keys.ESCAPE) {
                inputProcessor.getButtonIndexToKeyCode().inverse().forcePut(keycode, currentMenuOption.buttonIndex);
                menu = topLevelMenu;
                menu.activate();
            }
            return false;
        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            Gdx.app.log(getClass().getSimpleName(), "buttonDown(" + controller + ", " + buttonCode + ")");
            inputProcessor.getButtonIndexToButton().inverse().forcePut(buttonCode, currentMenuOption.buttonIndex);
            menu = topLevelMenu;
            menu.activate();
            return false;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            Gdx.app.log(getClass().getSimpleName(), "axisMoved(" + controller + ", " + axisCode + ", " + value + ")");
            if (Math.abs(value) >  .5f) {
                if (currentMenuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.LEFT ||
                        currentMenuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.RIGHT) {
                    inputProcessor.getActualAxisToAxisCode().inverse().forcePut(axisCode, KeyboardInputProcessor.Axis.X);
                } else if (currentMenuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.UP ||
                        currentMenuOption.buttonIndex == com.gradualgames.ggvm.Controller.Buttons.DOWN) {
                    inputProcessor.getActualAxisToAxisCode().inverse().forcePut(axisCode, KeyboardInputProcessor.Axis.Y);
                }
                menu = topLevelMenu;
                menu.activate();
            }
            return false;
        }

//        @Override
//        public boolean povMoved(Controller controller, int povCode, PovDirection value) {
//            Gdx.app.log(getClass().getSimpleName(), "povMoved(" + controller + ", " + povCode + ", " + value + ")");
//            //User decided to use a d-pad---clear out any axis configurations since these can conflict.
//            inputProcessor.getActualAxisToAxisCode().clear();
//            menu = topLevelMenu;
//            menu.activate();
//            return false;
//        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return menu.keyDown(keycode);
    }

    private abstract class MenuOption {

        public int x;
        public int y;
        public com.gradualgames.ggvm.Controller.Buttons buttonIndex;
        public String text;
        public MenuOption previousMenuOption;
        public MenuOption nextMenuOption;

        public MenuOption(int x, int y, com.gradualgames.ggvm.Controller.Buttons buttonIndex, String text) {
            this.x = x;
            this.y = y;
            this.buttonIndex = buttonIndex;
            this.text = text;
        }

        public abstract void action();
    }

    private class ButtonMenuOption extends MenuOption {

        public ButtonMenuOption(int x, int y, com.gradualgames.ggvm.Controller.Buttons buttonIndex, String text) {
            super(x, y, buttonIndex, text);
        }

        @Override
        public void action() {
            menu = promptInputMenu;
            promptInputMenu.activate();
        }
    }

    private class FullscreenWindowedToggleMenuOption extends MenuOption {

        public FullscreenWindowedToggleMenuOption(int x, int y, String text) {
            super(x, y, com.gradualgames.ggvm.Controller.Buttons.NONE, text);
        }

        @Override
        public void action() {
            boolean fullScreen = Gdx.graphics.isFullscreen();
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            if (fullScreen == true) {
                currentMenuOption.text = "TO FULLSCREEN";
                Gdx.graphics.setWindowedMode(512, 480);
            } else {
                currentMenuOption.text = "TO WINDOWED";
                Gdx.graphics.setFullscreenMode(currentMode);
            }
        }
    }

    private class DefaultsMenuOption extends MenuOption {
        public DefaultsMenuOption(int x, int y, String text) {
            super(x, y, com.gradualgames.ggvm.Controller.Buttons.NONE, text);
        }

        @Override
        public void action() {
            inputProcessor.clear();
            inputProcessor.autoConfigure();
        }
    }

    private class ReturnToTitleOption extends MenuOption {

        private boolean visited = false;

        public ReturnToTitleOption(int x, int y) {
            super(x, y, com.gradualgames.ggvm.Controller.Buttons.NONE, "RETURN TO TITLE");
        }

        public void resetVisited() {
            text = "RETURN TO TITLE";
            visited = false;
        }

        @Override
        public void action() {
            if (!visited) {
                text = "YOU WILL LOSE PROGRESS. OK?";
                visited = true;
            } else {
                resetVisited();
                soundtrackManager.stopSound();
                inputProcessor.activate();
                inputProcessor.saveConfiguration();
                FileHandle file = Gdx.files.local("state.sav");
                if (file.exists()) {
                    file.delete();
                }
                ggvm.reset();
                menu = topLevelMenu;
            }
        }
    }

    private class ExitMenuOption extends MenuOption {

        public ExitMenuOption(int x, int y, String text) {
            super(x, y, com.gradualgames.ggvm.Controller.Buttons.NONE, text);
        }

        @Override
        public void action() {
            soundtrackManager.unpauseMusic();
            inputProcessor.activate();
            inputProcessor.saveConfiguration();
            ggvm.start();
        }
    }

    private class SaveStateAndExitMenuOption extends MenuOption {

        public SaveStateAndExitMenuOption(int x, int y, String text) {
            super(x, y, com.gradualgames.ggvm.Controller.Buttons.NONE, text);
        }

        @Override
        public void action() {
            inputProcessor.saveConfiguration();
            Gdx.app.exit();
        }
    }
}

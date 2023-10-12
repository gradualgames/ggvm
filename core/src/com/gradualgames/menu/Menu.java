package com.gradualgames.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
//import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.input.InputProcessorBase;
import com.gradualgames.manager.soundtrack.SoundtrackManager;

/**
 * Created by derek on 11/20/2016.
 *
 * This is the base class for Menu objects. MenuManagers are for
 * displaying an options menu overtop of the rendered game graphics, for
 * configuring the controller or changing screen modes, etc.
 */
public abstract class Menu implements InputProcessor, ControllerListener {

    public void activate() {
        Gdx.input.setInputProcessor(this);
        Controllers.clearListeners();
        Controllers.addListener(this);
    }

    public void create() {}

    public void resize(int width, int height) {}

    public abstract void render(SpriteBatch spriteBatch);

    public void setDependencies(String fontFileName, String fontBitmapFileName, GGVm ggvm, InputProcessorBase inputProcessor, SoundtrackManager soundtrackManager) { }

    public static <T extends Menu> T newInstance(Class<T> menuClass) {
        try {
            T menu = menuClass.newInstance();
            return menu;
        } catch (InstantiationException e) {
            Gdx.app.error(Menu.class.getSimpleName(), e.getMessage());
        } catch (IllegalAccessException e) {
            Gdx.app.error(Menu.class.getSimpleName(), e.getMessage());
        }
        return null;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    //    @Override
//    public boolean scrolled(int amount) {
//        return false;
//    }

    @Override
    public void connected(Controller controller) {

    }

    @Override
    public void disconnected(Controller controller) {

    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        return false;
    }

//    @Override
//    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
//        return false;
//    }
//
//    @Override
//    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
//        return false;
//    }
//
//    @Override
//    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
//        return false;
//    }
//
//    @Override
//    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
//        return false;
//    }
}

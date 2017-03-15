package com.gradualgames.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.menu.Menu;

/**
 * Created by derek on 11/27/2016.
 *
 * A base class which implements everything in InputProcessor and ControllerListener, so
 * we can create extensions of this class which only override methods we need. It also
 * provides methods for setting dependencies such as the GGVm object and Menu object, and
 * methods for responding to the application lifecycle including rendering in the case of
 * mobile input processors.
 */
public abstract class InputProcessorBase implements InputProcessor, ControllerListener {

    public InputProcessorBase() {
    }

    public void setGGVm(GGVm ggvm) {

    }

    public void setMenu(Menu menu) {

    }

    public void loadConfiguration() {

    }

    public void saveConfiguration() {

    }

    public void render(SpriteBatch spriteBatch) {

    }

    public void resize(int width, int height) {

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
    public boolean scrolled(int amount) {
        return false;
    }

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

    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        return false;
    }

    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        return false;
    }

    public static <T extends InputProcessorBase> T newInstance(Class<T> inputProcessorClass, GGVm ggvm) {
        try {
            T inputProcessor = inputProcessorClass.newInstance();
            inputProcessor.setGGVm(ggvm);
            return inputProcessor;
        } catch (InstantiationException e) {
            Gdx.app.error(InputProcessorBase.class.getSimpleName(), e.getMessage());
        } catch (IllegalAccessException e) {
            Gdx.app.error(InputProcessorBase.class.getSimpleName(), e.getMessage());
        }
        return null;
    }
}

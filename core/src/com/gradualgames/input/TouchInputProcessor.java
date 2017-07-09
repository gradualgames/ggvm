package com.gradualgames.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.gradualgames.ggvm.Controller;
import com.gradualgames.ggvm.GGVm;

import java.util.*;

/**
 * Created by derek on 11/27/2016.
 *
 * This is the touch input processor for mobile devices. It contains
 * mappings between rectangles on the screen and actual ggvm buttons.
 * TODO: Find a way to keep the sizing of the controls consistent, so that
 * they aren't huge on large tablets. This may require that we actually
 * handle touch natively per platform and pass touch events down into GGVm,
 * but we should look first for a way to do this in a cross-platform fashion.
 */
public class TouchInputProcessor extends InputProcessorBase {

    private static final int MAX_TOUCHES = 8;
    private static final int DPAD_X = 55;
    private static final int DPAD_Y = 120;
    private static final int SELECT_X = 90;
    private static final int SELECT_Y = 20;
    private static final int START_X = 424 - 90;
    private static final int START_Y = 20;
    private static final int A_X = 380;
    private static final int A_Y = 130;
    private static final int B_X = 380;
    private static final int B_Y = 80;

    private GGVm ggvm;
    private StretchViewport overlayViewPort;
    private Camera overlayCamera;
    private ShapeRenderer shapeRenderer;
    private Texture dpadTexture;
    private Texture ssTexture;
    private Texture abTexture;

    private List<List<RectangleToButtonIndices>> rectangleToButtonIndicesList
            = new ArrayList<List<RectangleToButtonIndices>>();

    private Vector3 screenPoint = new Vector3();
    private Vector3 touchPoint = new Vector3();

    private String dpadFileName;
    private String ssFileName;
    private String abFileName;

    private class RectangleToButtonIndices {

        public Rectangle rectangle = new Rectangle();

        public List<Integer> buttonIndices = new ArrayList<Integer>();

        public Set<Integer> pointerSet = new HashSet<Integer>();
    }

    public TouchInputProcessor() {
        super();
        initializeRectangleToButtonIndicesList();
        initializeTouchRectangles();
    }

    @Override
    public void setGGVm(GGVm ggvm) {
        this.ggvm = ggvm;
    }

    public void setDPadFileName(String dPadFileName) {
        this.dpadFileName = dPadFileName;
    }

    public void setSSFileName(String ssFileName) {
        this.ssFileName = ssFileName;
    }

    public void setABFileName(String abFileName) {
        this.abFileName = abFileName;
    }

    @Override
    public void create() {
        overlayCamera = new OrthographicCamera(424, 240);
        overlayCamera.translate(overlayCamera.viewportWidth / 2, overlayCamera.viewportHeight / 2, 0);
        overlayCamera.update();
        overlayViewPort = new StretchViewport(424, 240, overlayCamera);
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        initializeTextures();
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        overlayViewPort.apply();
        drawTextures(spriteBatch);
    }

    @Override
    public void resize(int width, int height) {
        overlayViewPort.update(width, height);
    }

    private void drawRectangles() {
        shapeRenderer.setProjectionMatrix(overlayCamera.combined);
        shapeRenderer.begin();
        shapeRenderer.setColor(.5f, .5f, .5f, .5f);
        for(RectangleToButtonIndices rectangleToButtonIndices: rectangleToButtonIndicesList.get(0)) {
            shapeRenderer.rect(rectangleToButtonIndices.rectangle.x, rectangleToButtonIndices.rectangle.y, rectangleToButtonIndices.rectangle.width, rectangleToButtonIndices.rectangle.height);
        }
        shapeRenderer.end();
    }

    private void drawTextures(SpriteBatch spriteBatch) {
        spriteBatch.setProjectionMatrix(overlayCamera.combined);
        spriteBatch.begin();
        drawCenteredTexture(spriteBatch, dpadTexture, DPAD_X, DPAD_Y, 100, 100);
        drawCenteredTexture(spriteBatch, ssTexture, SELECT_X, SELECT_Y, 40, 20);
        drawCenteredTexture(spriteBatch, ssTexture, START_X, START_Y, 40, 20);
        drawCenteredTexture(spriteBatch, abTexture, A_X, A_Y, 40, 40);
        drawCenteredTexture(spriteBatch, abTexture, B_X, B_Y, 40, 40);
        spriteBatch.end();
    }

    private void drawCenteredTexture(SpriteBatch spriteBatch, Texture texture, int x, int y, int w, int h) {
        spriteBatch.draw(texture, x - w/2, y - h/2, w, h);
    }

    private void initializeRectangleToButtonIndicesList() {
        for(int i = 0; i < MAX_TOUCHES; i++) {
            rectangleToButtonIndicesList.add(new ArrayList<RectangleToButtonIndices>());
        }
    }

    private void initializeTextures() {
        dpadTexture = new Texture(dpadFileName);
        ssTexture = new Texture(ssFileName);
        abTexture = new Texture(abFileName);
    }

    private void initializeTouchRectangles() {
        addDpad(DPAD_X, DPAD_Y, 35);
        addButton(A_X, A_Y, 20, Arrays.asList(Controller.Buttons.A.ordinal()));
        addButton(B_X, B_Y, 20, Arrays.asList(Controller.Buttons.B.ordinal()));
        addButton(START_X, START_Y, 20, 10, Arrays.asList(Controller.Buttons.START.ordinal()));
        addButton(SELECT_X, SELECT_Y, 20, 10, Arrays.asList(Controller.Buttons.SELECT.ordinal()));
    }

    private void addDpad(int centerX, int centerY, int radius) {
        addButton(centerX - radius, centerY, radius / 2, Arrays.asList(Controller.Buttons.LEFT.ordinal()));
        addButton(centerX + radius, centerY, radius / 2, Arrays.asList(Controller.Buttons.RIGHT.ordinal()));
        addButton(centerX, centerY + radius, radius / 2, Arrays.asList(Controller.Buttons.UP.ordinal()));
        addButton(centerX, centerY - radius, radius / 2, Arrays.asList(Controller.Buttons.DOWN.ordinal()));

        int diagonalRadius = radius - 8;
        addButton(centerX - diagonalRadius, centerY - diagonalRadius, radius / 2, Arrays.asList(Controller.Buttons.LEFT.ordinal(), Controller.Buttons.DOWN.ordinal()));
        addButton(centerX - diagonalRadius, centerY + diagonalRadius, radius / 2, Arrays.asList(Controller.Buttons.LEFT.ordinal(), Controller.Buttons.UP.ordinal()));
        addButton(centerX + diagonalRadius, centerY - diagonalRadius, radius / 2, Arrays.asList(Controller.Buttons.RIGHT.ordinal(), Controller.Buttons.DOWN.ordinal()));
        addButton(centerX + diagonalRadius, centerY + diagonalRadius, radius / 2, Arrays.asList(Controller.Buttons.RIGHT.ordinal(), Controller.Buttons.UP.ordinal()));
    }

    private void addButton(int centerX, int centerY, int radius, List<Integer> buttonIndices) {
        addButton(centerX, centerY, radius, radius, buttonIndices);
    }

    private void addButton(int centerX, int centerY, int radiusX, int radiusY, List<Integer> buttonIndices) {
        RectangleToButtonIndices rectangleToButtonIndices = new RectangleToButtonIndices();
        rectangleToButtonIndices.rectangle = new Rectangle(centerX - radiusX, centerY - radiusY, radiusX * 2, radiusY * 2);
        for(Integer buttonIndex: buttonIndices) {
            rectangleToButtonIndices.buttonIndices.add(buttonIndex);
        }
        for(int i = 0; i < MAX_TOUCHES; i++) {
            rectangleToButtonIndicesList.get(i).add(rectangleToButtonIndices);
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (pointer < rectangleToButtonIndicesList.size()) {
            screenPoint.set(screenX, screenY, 0);
            touchPoint = overlayViewPort.unproject(screenPoint);

            for (RectangleToButtonIndices rectangleToButtonIndices : rectangleToButtonIndicesList.get(pointer)) {
                if (rectangleToButtonIndices.rectangle.contains(touchPoint.x, touchPoint.y)) {
                    rectangleToButtonIndices.pointerSet.add(pointer);
                }
            }
        }
        updateButtons();
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer < rectangleToButtonIndicesList.size()) {
            screenPoint.set(screenX, screenY, 0);
            touchPoint = overlayViewPort.unproject(screenPoint);

            for (RectangleToButtonIndices rectangleToButtonIndices : rectangleToButtonIndicesList.get(pointer)) {
                if (rectangleToButtonIndices.rectangle.contains(touchPoint.x, touchPoint.y)) {
                    rectangleToButtonIndices.pointerSet.add(pointer);
                } else {
                    rectangleToButtonIndices.pointerSet.remove(pointer);
                }
            }
        }
        updateButtons();
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer < rectangleToButtonIndicesList.size()) {
            for (RectangleToButtonIndices rectangleToButtonIndices : rectangleToButtonIndicesList.get(pointer)) {
                rectangleToButtonIndices.pointerSet.remove(pointer);
            }
        }
        updateButtons();
        return true;
    }

    private void updateButtons() {
        for(int i = 0; i < 8; i++) {
            ggvm.setButtonState(i, false);
        }

        for (RectangleToButtonIndices rectangleToButtonIndices : rectangleToButtonIndicesList.get(0)) {
            for (Integer buttonIndex : rectangleToButtonIndices.buttonIndices) {
                boolean pointersPresentOnThisRect = rectangleToButtonIndices.pointerSet.size() > 0;
                if (pointersPresentOnThisRect) {
                    ggvm.setButtonState(buttonIndex, true);
                }
            }
        }
    }
}

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
import com.gradualgames.ggvm.GGVmRegisterHideMobileButtons;

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
    private static final int START_X = -90;
    private static final int START_Y = 20;
    private static final int SIZE_TOGGLE_X = 40;
    private static final int SIZE_TOGGLE_Y = -20;
    private static final int A_X = -44;
    private static final int A_Y = 130;
    private static final int B_X = -44;
    private static final int B_Y = 80;

    private static final int ACTION_CYCLE_SCALE = 0;

    private GGVm ggvm;
    private GGVmRegisterHideMobileButtons ggVmRegisterHideMobileButtons = new GGVmRegisterHideMobileButtons();
    private StretchViewport overlayViewPort;
    private Camera overlayCamera;
    private ShapeRenderer shapeRenderer;
    private Texture dpadTexture;
    private Texture ssTexture;
    private Texture abTexture;

    private float scale = 1f;

    private boolean recreateButtons = false;

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

        public List<Integer> actions = new ArrayList<Integer>();
    }

    public TouchInputProcessor() {
        super();
        initializeRectangleToButtonIndicesList();
        initializeTouchRectangles();
    }

    @Override
    public void setGGVm(GGVm ggvm) {
        this.ggvm = ggvm;
        ggvm.installReadWriteRange(ggVmRegisterHideMobileButtons);
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
        drawRectangles();
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
        if (!ggVmRegisterHideMobileButtons.getHideDPad()) {
            drawCenteredTexture(spriteBatch, dpadTexture, DPAD_X * scale, DPAD_Y * scale, 100 * scale, 100 * scale);
        }
        if (!ggVmRegisterHideMobileButtons.getHideSelect()) {
            drawCenteredTexture(spriteBatch, ssTexture, SELECT_X * scale, SELECT_Y * scale, 40 * scale, 20 * scale);
        }
        if (!ggVmRegisterHideMobileButtons.getHideStart()) {
            drawCenteredTexture(spriteBatch, ssTexture, 424 + START_X * scale, START_Y * scale, 40 * scale, 20 * scale);
        }
        drawCenteredTexture(spriteBatch, ssTexture, SIZE_TOGGLE_X , 240 + SIZE_TOGGLE_Y , 40, 20);
        if (!ggVmRegisterHideMobileButtons.getHideA()) {
            drawCenteredTexture(spriteBatch, abTexture, 424 + A_X * scale, A_Y * scale, 40 * scale, 40 * scale);
        }
        if (!ggVmRegisterHideMobileButtons.getHideB()) {
            drawCenteredTexture(spriteBatch, abTexture, 424 + B_X * scale, B_Y * scale, 40 * scale, 40 * scale);
        }
        spriteBatch.end();
    }

    private void drawCenteredTexture(SpriteBatch spriteBatch, Texture texture, float x, float y, float w, float h) {
        spriteBatch.draw(texture, (x - w/2), (y - h/2), w, h);
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
        addDpad(DPAD_X * scale, DPAD_Y * scale, 35 * scale);
        addButton(424 + A_X * scale, A_Y * scale, 20 * scale, Arrays.asList(Controller.Buttons.A.ordinal()), new ArrayList<Integer>());
        addButton(424 + B_X * scale, B_Y * scale, 20 * scale, Arrays.asList(Controller.Buttons.B.ordinal()), new ArrayList<Integer>());
        addButton(424 + START_X * scale, START_Y * scale, 20 * scale, 10 * scale, Arrays.asList(Controller.Buttons.START.ordinal()), new ArrayList<Integer>());
        addButton(SELECT_X * scale, SELECT_Y * scale, 20 * scale, 10 * scale, Arrays.asList(Controller.Buttons.SELECT.ordinal()), new ArrayList<Integer>());

        addButton(SIZE_TOGGLE_X, 240 + SIZE_TOGGLE_Y, 20, 10, new ArrayList<Integer>(), Arrays.asList(ACTION_CYCLE_SCALE));
    }

    private void addDpad(float centerX, float centerY, float radius) {
        float dpadButtonScale = 2.2f;
        addButton(centerX - radius, centerY, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.LEFT.ordinal()), new ArrayList<Integer>());
        addButton(centerX + radius, centerY, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.RIGHT.ordinal()), new ArrayList<Integer>());
        addButton(centerX, centerY + radius, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.UP.ordinal()), new ArrayList<Integer>());
        addButton(centerX, centerY - radius, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.DOWN.ordinal()), new ArrayList<Integer>());

        float diagonalRadius = radius - (8 * scale);
        addButton(centerX - diagonalRadius, centerY - diagonalRadius, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.LEFT.ordinal(), Controller.Buttons.DOWN.ordinal()), new ArrayList<Integer>());
        addButton(centerX - diagonalRadius, centerY + diagonalRadius, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.LEFT.ordinal(), Controller.Buttons.UP.ordinal()), new ArrayList<Integer>());
        addButton(centerX + diagonalRadius, centerY - diagonalRadius, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.RIGHT.ordinal(), Controller.Buttons.DOWN.ordinal()), new ArrayList<Integer>());
        addButton(centerX + diagonalRadius, centerY + diagonalRadius, radius / dpadButtonScale, Arrays.asList(Controller.Buttons.RIGHT.ordinal(), Controller.Buttons.UP.ordinal()), new ArrayList<Integer>());
    }

    private void addButton(float centerX, float centerY, float radius, List<Integer> buttonIndices, List<Integer> actions) {
        addButton(centerX, centerY, radius, radius, buttonIndices, actions);
    }

    private void addButton(float centerX, float centerY, float radiusX, float radiusY, List<Integer> buttonIndices, List<Integer> actions) {
        RectangleToButtonIndices rectangleToButtonIndices = new RectangleToButtonIndices();
        rectangleToButtonIndices.rectangle = new Rectangle(centerX - radiusX, centerY - radiusY, radiusX * 2, radiusY * 2);
        for(Integer buttonIndex: buttonIndices) {
            rectangleToButtonIndices.buttonIndices.add(buttonIndex);
        }
        for (Integer action: actions) {
            rectangleToButtonIndices.actions.add(action);
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
            for (Integer action: rectangleToButtonIndices.actions) {
                boolean pointersPresentOnThisRect = rectangleToButtonIndices.pointerSet.size() > 0;
                if (pointersPresentOnThisRect) {
                    if (action == ACTION_CYCLE_SCALE) {
                        if (scale == 1.0f) scale = .75f;
                        else if (scale == .75f) scale = .5f;
                        else if (scale == .5f) scale = 1f;
                        recreateButtons = true;
                    }
                }
            }
        }

        if (recreateButtons) {
            rectangleToButtonIndicesList.clear();
            initializeRectangleToButtonIndicesList();
            initializeTouchRectangles();
            recreateButtons = false;
        }
    }
}

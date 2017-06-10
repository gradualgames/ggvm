package com.gradualgames.manager.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.OnGeneratePatternTableListener;
import com.gradualgames.manager.rastereffect.RasterEffectManager;

/**
 * Created by derek on 1/7/2017.
 *
 * This is the RenderManager, one of the most important objects in GGVm.
 * It uses the high level interface of the GGVm object itself to generate
 * graphics from ppu data and update the palette every frame. When rendering,
 * it draws background sprites first, then the nametable, and finally the fore-
 * ground sprites. To simulate the ability to hide foreground sprites with
 * background sprites, foreground sprites are drawn to a framebuffer first, then
 * background sprites are drawn as transparent 8x8 rectangles to erase pixels of
 * the foreground sprites.
 *
 * Backgrounds are rendered with HorizontalMirroringRenderManager and
 * VerticalMirroringRenderManager. They use various ppu register information,
 * forwarded via the GGVm object, to determine how to draw each tile. Note this
 * does not simulate scanline per scanline drawing of the PPU. Highly advanced
 * raster effects may be impossible or require a fragment shader to be written,
 * and then inspect the ram of the running game to inform the behavior of the
 * shader. Split screens should be supportable by inspecting the ram of the
 * running game and then drawing clipped portions of the screen to separate fbos
 * using similar techniques to the above mentioned classes.
 *
 * Sprites are drawn in priority order. However the above described approach is not
 * accurate to how the PPU actually draws graphics, but is able to support the needs
 * of several homebrew games so far.
 * TODO: Find a way to use a z-buffer and blending in order to accurately simulate
 * how the PPU sorts sprites and background pixels. In theory this could also be
 * faster as we wouldn't be blending a whole framebuffer on top of the screen.
 *
 * All chr graphics are generated into a 512x256 sized texture, which has four copies
 * each of pattern table 0 and pattern table 1. The pixels generated into this texture
 * are encoded with special rgb values which direct a fragment shader to look up a
 * color in a palette texture, which is generated every frame. In this way, palette
 * animation is supported. The duplication of the pattern tables was needed so that the
 * attribute information is available immediately per pixel to the fragment shader, obtaining
 * good performance and as few flushes as possible.
 *
 */
public abstract class RenderManager {

    protected GGVm ggvm;
    protected RasterEffectManager rasterEffectManager;
    protected PatternTableManager patternTableManager;

    //LibGDX objects
    protected FitViewport viewPort;
    protected Camera camera;
    protected int backgroundSpritesCount = 0;
    protected int foregroundSpritesCount = 0;
    protected int[] backgroundSprites = new int[64];
    protected int[] foregroundSprites = new int[64];
    protected boolean[] isBgTransparentMask = new boolean[64];
    protected boolean[] isSprTransparentMask = new boolean[64];
    protected float[] attributes = new float[4];
    protected Color bgColor = Color.BLACK;
    protected ShapeRenderer shapeRenderer;
    protected Pixmap palettePixmap;
    protected Texture paletteTexture;

    protected Pixmap transparentMaskPixmap;
    protected Texture transparentMaskTexture;
    protected Sprite transparentMaskSprite;
    protected FrameBuffer mainFrameBuffer;
    protected TextureRegion mainTextureRegion;
    protected FrameBuffer foregroundSpritesFrameBuffer;
    protected TextureRegion foregroundSpritesTextureRegion;
    protected FPSLogger fpsLogger = new FPSLogger();

    //Shader objects
    protected String vertexShader;
    protected String fragmentShader;
    protected ShaderProgram shaderProgram;

    //Palette information
    protected int[] masterPalette = new int[64];

    public RenderManager(GGVm ggvm, PatternTableManager patternTableManager, RasterEffectManager rasterEffectManager) {
        this.ggvm = ggvm;
        this.patternTableManager = patternTableManager;
        this.rasterEffectManager = rasterEffectManager;
        initialize();
    }

    private void initialize() {
        //LibGDX setup
        camera = new OrthographicCamera();
        viewPort = new FitViewport(256, 240, camera);
        viewPort.apply();
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        //Initialize shaders
        vertexShader = Gdx.files.internal("shaders/vertex_shader.glsl").readString();
        fragmentShader = Gdx.files.internal("shaders/fragment_shader.glsl").readString();
        shaderProgram = new ShaderProgram(vertexShader, fragmentShader);

        //Initialize attribute value (used by fragmentShader)
        initializeAttributes();

        //Make the mouse cursor transparent
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.argb8888(0, 0, 0, 0));
        pixmap.fill();
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(pixmap, 0, 0));
        pixmap.dispose();

        //Initialize transparent mask sprite
        transparentMaskPixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        transparentMaskPixmap.setColor(Color.CLEAR);
        transparentMaskPixmap.fill();
        transparentMaskTexture = new Texture(transparentMaskPixmap);
        transparentMaskSprite = new Sprite(transparentMaskTexture);

        //Initialize main frame buffer
        mainFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 240, false);
        mainTextureRegion = new TextureRegion(mainFrameBuffer.getColorBufferTexture(), 0, 0,
                256, 240);
        mainTextureRegion.flip(false, true);
        mainTextureRegion.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        //Initialize special frame buffer for foreground sprites so
        //we can hide them with fake background sprites
        foregroundSpritesFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 240, false);
        foregroundSpritesTextureRegion = new TextureRegion(foregroundSpritesFrameBuffer.getColorBufferTexture(), 0, 0,
                256, 240);
        foregroundSpritesTextureRegion.flip(false, true);
        foregroundSpritesTextureRegion.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        //Initialize palette information
        palettePixmap = new Pixmap(32, 1, Pixmap.Format.RGBA8888);
        paletteTexture = new Texture(palettePixmap);
        masterPalette = loadPalette("palette/nespalette.bmp", 1, 1, 32, 32, 16, 4);
    }

    public void resize(int width, int height) {
        viewPort.update(width, height);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();
    }

    public void render(SpriteBatch spriteBatch) {
        generatePalettes();
        paletteTexture.bind(1);

        //Clear main framebuffer.
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //Fill just the 256x240 viewport area with the current bg color
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin();
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(0f, 0f, 256f, 240f);
        shapeRenderer.end();

        if (ggvm.isBackgroundVisible()) {

            sortSprites();
            //Draw foreground sprites to a separate framebuffer, where they can be
            //optionally clipped by background sprites.
            foregroundSpritesFrameBuffer.begin();
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
                Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                spriteBatch.setShader(shaderProgram);
                spriteBatch.setProjectionMatrix(camera.combined);
                spriteBatch.begin();
                    shaderProgram.setUniformi("u_paletteTexture", 1);
                    spriteBatch.disableBlending();
                    drawSprites(spriteBatch, foregroundSprites, isSprTransparentMask, foregroundSpritesCount);
                spriteBatch.end();
            foregroundSpritesFrameBuffer.end();

            viewPort.apply();
            mainFrameBuffer.begin();
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
                Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                //Draw background sprites, nametable, then blend
                //the foreground sprites framebuffer to the main framebuffer.
                spriteBatch.begin();
                    spriteBatch.enableBlending();
                    drawSprites(spriteBatch, backgroundSprites, isBgTransparentMask, backgroundSpritesCount);
                    drawNametable(ggvm, spriteBatch);
                    spriteBatch.setShader(null);
                    spriteBatch.draw(foregroundSpritesTextureRegion, 0, 0);
                    rasterEffectManager.render(spriteBatch);
                spriteBatch.end();
            mainFrameBuffer.end();

            viewPort.apply();
            spriteBatch.begin();
                spriteBatch.draw(mainTextureRegion, 0, 0);
            spriteBatch.end();

            if (ggvm.isBackgroundClipping()) {
                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.begin();
                shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(bgColor);
                shapeRenderer.rect(0f, 0f, 8f, 240f);
                shapeRenderer.end();
            }
        }
        fpsLogger.log();
    }

    /**
     * Generate palette textures for background and sprites. These textures are
     * used by the shader to look up which color to replace. Actual pixels are
     * set up such that the r component is a value between 0 and .25, of .25/4
     * increments, the g component is a value between 0 and 1 of .25 increments
     * (corresponding to the attribute). Then these r and g components are used by
     * the shader to pick the correct color from the palette texture generated here.
     */
    private void generatePalettes() {
        int[] bgGGVmPalette = ggvm.getPalette(false);
        for (int i = 0; i < 16; i++) {
            if (i == 0) {
                bgColor = new Color(masterPalette[bgGGVmPalette[i]]);
            }
            palettePixmap.setColor(masterPalette[bgGGVmPalette[i]]);
            palettePixmap.fillRectangle(i, 0, 1, 1);
        }

        int[] sprGGVmPalette = ggvm.getPalette(true);
        for (int i = 0; i < 16; i++) {
            Color color = new Color(masterPalette[sprGGVmPalette[i]]);
            if (i % 4 == 0) {
                color.set(color.r, color.g, color.b, 0f);
            }
            palettePixmap.setColor(color);
            palettePixmap.fillRectangle(i + 16, 0, 1, 1);
        }

        paletteTexture.draw(palettePixmap, 0, 0);
    }

    /**
     * Draws the current state of the two nametables in ggvm, taking scrolling into
     * account.
     */
    protected abstract void drawNametable(GGVm ggvm, SpriteBatch spriteBatch);

    /**
     * Sort sprites into those which are behind the background tiles
     * and those which are in front of the background tiles.
     */
    private void sortSprites() {
        backgroundSpritesCount = 0;
        foregroundSpritesCount = 0;
        for (int i = 63; i >= 0; i--) {
            boolean behindBackground = ggvm.getSpriteIsBehindBackground(i);
            if (behindBackground) {
                backgroundSprites[backgroundSpritesCount] = i;
                isBgTransparentMask[backgroundSpritesCount] = false;
                backgroundSpritesCount++;

                foregroundSprites[foregroundSpritesCount] = i;
                isSprTransparentMask[foregroundSpritesCount] = true;
                foregroundSpritesCount++;
            } else {
                foregroundSprites[foregroundSpritesCount] = i;
                isSprTransparentMask[foregroundSpritesCount] = false;
                foregroundSpritesCount++;
            }
        }
    }

    /**
     * Draw a list of sprites using their indices.
     */
    private void drawSprites(SpriteBatch spriteBatch, int[] spriteIndices, boolean[] isTransparentMask, int spriteCount) {
        int patternTable = ggvm.getSpritePatternTableAddress() == 0 ?
                0 : 1;

        if(ggvm.getSpriteSize() == 0) {
            for (int i = 0; i < spriteCount; i++) {
                int spriteRamIndex = spriteIndices[i];
                int tile = ggvm.getSpriteTile(spriteRamIndex);
                int attribute = ggvm.getSpriteColorAttribute(spriteRamIndex);
                int x = ggvm.getSpriteX(spriteRamIndex);
                int y = ggvm.getSpriteY(spriteRamIndex);
                boolean horizontalFlip = ggvm.getSpriteHorizontalFlip(spriteRamIndex);
                boolean verticalFlip = ggvm.getSpriteVerticalFlip(spriteRamIndex);
                if (y != 0xff) {
                    if (!isTransparentMask[i]) {
                        spriteBatch.enableBlending();
                        int indexRow = tile >> 4;
                        int indexColumn = tile & 0x0f;
                        Sprite sprite = patternTableManager.getSprite(patternTable, indexRow, indexColumn);//patternTableSprites[patternTableOffset * 16 + indexRow][indexColumn];
                        sprite.setColor(0, attributes[attribute], .5f, 0);
                        sprite.setPosition(x, 231 - y);
                        sprite.setFlip(horizontalFlip, verticalFlip);
                        sprite.draw(spriteBatch);
                    } else {
                        spriteBatch.disableBlending();
                        transparentMaskSprite.setPosition(x, 231 - y);
                        transparentMaskSprite.draw(spriteBatch);
                    }
                }
            }
        } else if (ggvm.getSpriteSize() == 1) {
            for (int spriteIndex = 0; spriteIndex < spriteCount; spriteIndex++) {
                int i = spriteIndices[spriteIndex];
                int index = ggvm.getSpriteTile(i);
                int attribute = ggvm.getSpriteColorAttribute(i);
                int x = ggvm.getSpriteX(i);
                int y = ggvm.getSpriteY(i);
                boolean horizontalFlip = ggvm.getSpriteHorizontalFlip(i);
                boolean verticalFlip = ggvm.getSpriteVerticalFlip(i);
                if (y != 0xff) {
                    int indexRow = index >> 4;
                    int indexColumn = index & 0x0f;
                    Sprite sprite = patternTableManager.getSprite(patternTable, indexRow, indexColumn);//patternTableSprites[patternTableOffset * 16 + indexRow][indexColumn];
                    sprite.setColor(0, attributes[attribute], .5f, 0);
                    sprite.setPosition(x, 239 - y - 16);
                    sprite.setFlip(horizontalFlip, verticalFlip);
                    sprite.draw(spriteBatch);

                    int secondIndex = (index - 1) & 0xff;
                    indexRow = secondIndex >> 4;
                    indexColumn = secondIndex & 0x0f;
                    sprite = patternTableManager.getSprite(patternTable, indexRow, indexColumn); //patternTableSprites[patternTableOffset * 16 + indexRow][indexColumn];
                    sprite.setColor(0, attributes[attribute], .5f, 0);
                    sprite.setPosition(x, 231 - y);
                    sprite.setFlip(horizontalFlip, verticalFlip);
                    sprite.draw(spriteBatch);
                }
            }
        }
    }

    /**
     * Loads the NES master palette by sampling a bitmap that is a visual
     * representation of the NES masterPalette. Thus this method traverses the bitmap
     * color cell by color cell sampling one pixel from each. The method is configurable
     * to be able to traverse any rectangular representation of the NES masterPalette to build
     * the master masterPalette.
     *
     * @param paletteFile
     * @param topLeftX
     * @param topLeftY
     * @param xStep
     * @param yStep
     * @param columns
     * @param rows
     * @return
     */
    private int[] loadPalette(String paletteFile, int topLeftX, int topLeftY, int xStep, int yStep, int columns, int rows) {
        Gdx.app.log(getClass().getSimpleName(), "loadPalette()");
        int[] palette = new int[256];
        Texture texture = new Texture(paletteFile);
        texture.getTextureData().prepare();
        Pixmap pixmap = texture.getTextureData().consumePixmap();
        int paletteIndex = 0;
        for (int imageY = topLeftY; imageY < yStep * rows; imageY += yStep) {
            for (int imageX = topLeftX; imageX < xStep * columns; imageX += xStep) {
                int color = pixmap.getPixel(imageX, imageY);
                //Record and mirror the colors in case palettes are read uninitialized
                palette[paletteIndex] = color;
                palette[paletteIndex + 64] = color;
                palette[paletteIndex + 128] = color;
                palette[paletteIndex + 192] = color;
                paletteIndex++;
            }
        }
        return palette;
    }

    /**
     * Initialize attribute values used for the shader. These values
     * are used with setColor when drawing Sprite objects (both for
     * backgrounds and sprites with respect to ggvm) to set the green
     * channel to guide the shader as to which portion of the palette
     * lookup texture to use. The blue channel is set to 0 for backgrounds
     * and .5 for sprites, as well.
     */
    private void initializeAttributes() {
        for(int attribute = 0; attribute < 4; attribute++) {
            attributes[attribute] = (((float) attribute) * .25f) / 2f;
        }
    }
}

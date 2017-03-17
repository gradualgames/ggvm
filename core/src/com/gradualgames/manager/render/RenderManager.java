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
import com.gradualgames.ggvm.OnGenerateGraphicsListener;
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
 * TODO: Implement CHR-ROM bankswitching. We would have to generate more backing textures in
 * various configurations depending on the mapper, but this should not be very difficult to
 * implement as PRG-ROM bankswitching already works well (see Mapper 2 and RomSwitchboard).
 *
 * TODO: Implement live CHR-RAM updates. This may prove to be tricky or impossible especially on
 * mobile devices. In theory it should be possible to use an FBO insteada of a texture and modify
 * its pixels on every frame. We could wire up the PpuBus to fire events whenever it is invalidated,
 * and regenerate just the CHR tiles that were affected.
 */
public abstract class RenderManager implements OnGenerateGraphicsListener {

    protected GGVm ggvm;
    protected RasterEffectManager rasterEffectManager;

    //LibGDX objects
    protected FitViewport viewPort;
    protected Camera camera;
    protected Pixmap patternTablePixmap;
    protected Texture patternTableTexture;
    protected Sprite[][] patternTableSprites = new Sprite[32][64];
    protected int backgroundSpritesCount = 0;
    protected int foregroundSpritesCount = 0;
    protected int[] backgroundSprites = new int[64];
    protected int[] foregroundSprites = new int[64];
    protected boolean[] isBgTransparentMask = new boolean[64];
    protected boolean[] isSprTransparentMask = new boolean[64];
    protected Color bgColor = Color.BLACK;
    protected ShapeRenderer shapeRenderer;
    protected Pixmap palettePixmap;
    protected Texture paletteTexture;

    protected Pixmap transparentMaskPixmap;
    protected Texture transparentMaskTexture;
    protected Sprite transparentMaskSprite;
    protected FrameBuffer foregroundSpritesFrameBuffer;
    protected TextureRegion foregroundSpritesTextureRegion;
    protected FPSLogger fpsLogger = new FPSLogger();

    //Shader objects
    protected String vertexShader;
    protected String fragmentShader;
    protected ShaderProgram shaderProgram;

    //Palette information
    protected int[] masterPalette = new int[64];
    protected int[][] bgMonochromePalette = new int[4][4];
    protected int[][] sprMonochromePalette = new int[4][4];

    public RenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager) {
        this.ggvm = ggvm;
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

        //Make the mouse cursor transparent
        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.argb8888(0, 0, 0, 0));
        pixmap.fill();
        Gdx.graphics.setCursor(Gdx.graphics.newCursor(pixmap, 0, 0));
        pixmap.dispose();

        //Allocate a pixmap big enough to accommodate four copies of the
        //bg pattern table for each attribute in 128x128 squares in one row,
        //followed by four copies of the spr pattern table for each attribute
        //in 128x128 squares in the second row.
        patternTablePixmap = new Pixmap(512, 256, Pixmap.Format.RGBA8888);
        //Set blending to none so we can rewrite the pixmap and draw it to the
        //pattern table texture when graphics are regenerated.
        patternTablePixmap.setBlending(Pixmap.Blending.None);

        patternTableTexture = new Texture(patternTablePixmap, false);
        TextureRegion[][] textureRegions = TextureRegion.split(patternTableTexture, 8, 8);
        for(int row = 0; row < 32; row++) {
            for(int column = 0; column < 64; column++) {
                TextureRegion textureRegion = textureRegions[row][column];
                fixBleeding(textureRegion);
                patternTableSprites[row][column] = new Sprite(textureRegion);
            }
        }

        //Initialize transparent mask sprite
        transparentMaskPixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        transparentMaskPixmap.setColor(Color.CLEAR);
        transparentMaskPixmap.fill();
        transparentMaskTexture = new Texture(transparentMaskPixmap);
        transparentMaskSprite = new Sprite(transparentMaskTexture);

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
        initializeMonochromePalettes();
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

            //Draw background sprites, nametable, then blend
            //the foreground sprites framebuffer to the main framebuffer.
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
                spriteBatch.enableBlending();
                drawSprites(spriteBatch, backgroundSprites, isBgTransparentMask, foregroundSpritesCount);
                drawNametable(ggvm, spriteBatch);
                spriteBatch.setShader(null);
                spriteBatch.draw(foregroundSpritesTextureRegion, 0, 0);
                rasterEffectManager.render(spriteBatch);
            spriteBatch.end();
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
        int patternTableOffset = ggvm.getSpritePatternTableAddress() == 0 ?
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
                        Sprite sprite = patternTableSprites[patternTableOffset * 16 + indexRow][attribute * 16 + indexColumn];
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
                    Sprite sprite = patternTableSprites[patternTableOffset * 16 + indexRow][attribute * 16 + indexColumn];
                    sprite.setPosition(x, 239 - y - 16);
                    sprite.setFlip(horizontalFlip, verticalFlip);
                    sprite.draw(spriteBatch);

                    int secondIndex = (index - 1) & 0xff;
                    indexRow = secondIndex >> 4;
                    indexColumn = secondIndex & 0x0f;
                    sprite = patternTableSprites[patternTableOffset * 16 + indexRow][attribute * 16 + indexColumn];
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
     * Initializes monochrome palettes used when generating textures from chr data.
     * GGVm palettes consist of 4 sets of 4 colors. Each set of 4 is considered an
     * attribute. The g component of every pixel is set up to pick an attribute by
     * setting it to the values 0, .25, .5 and .75, to be used as part of a u,v coordinate
     * by the shader to pick the correct color. The r component consists of further
     * subdivisions of 4 in increments of .25/4, added to the g component to pick the
     * correct color from the background or sprite palette.
     * <p>
     * For the sprite palette, the alpha channel is set to be transparent for the first
     * of every set of four colors.
     */
    private void initializeMonochromePalettes() {
        Gdx.app.log(getClass().getSimpleName(), "initializeMonochromePalettes()");
        for (int attributeValue = 0; attributeValue <= 3; attributeValue++) {
            for (int pixelValue = 0; pixelValue <= 3; pixelValue++) {
                bgMonochromePalette[attributeValue][pixelValue] = pixelToShaderPixel(0f, pixelValue, attributeValue, pixelValue == 0 ? true : false);
            }

            for (int pixelValue = 0; pixelValue <= 3; pixelValue++) {
                sprMonochromePalette[attributeValue][pixelValue] = pixelToShaderPixel(.5f, pixelValue, attributeValue, pixelValue == 0 ? true : false);
            }
        }
    }

    /**
     * Converts a monochrome pixel value (expected to be 0 to 3), attribute value,
     * (also expected to be 0 to 3) into a color to be consumed by the fragment shader.
     * The r and g components are used to look up the actual color to replace the pixel
     * with within a palette texture looked up by u/v coordinates in the fragment shader.
     * The g component will be the attribute and will point to the base of one of 4,
     * 4 color palettes with the values 0, .25, .5 and .75. The actual color will be pointed
     * to by the r component as an offset from the g component, which is .25/8 + the
     * attribute value.
     *
     * @param pixelValue
     * @param attributeValue
     * @param transparentColor
     * @return
     */
    private int pixelToShaderPixel(float offset, int pixelValue, int attributeValue, boolean transparentColor) {
        float r = (((float) pixelValue) * (.25f / 4f)) + (.25f / 8f);
        float g = ((float) attributeValue) * .25f;
        float b = offset;
        return Color.rgba8888(r / 2, g / 2, b, transparentColor ? 0f : 1f);
    }

    /**
     * Callback from ggvm which tells the application to generate tile graphics based
     * on data in ggvm.
     */
    @Override
    public void onGenerateGraphics() {
        Gdx.app.log(getClass().getSimpleName(), "onGenerateGraphics()");
        generateSpritesForPatternTable();
    }

    /**
     * Generates textures and sprites based on pattern table data in ggvm.
     */
    private void generateSpritesForPatternTable() {
        //Iterate over current pattern table in tile units
        for(int row = 0; row < 16; row++) {
            for(int column = 0; column < 16; column++) {
                //Iterate over current tile in pixel units
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        for(int patternTableSelector = 0; patternTableSelector < 2; patternTableSelector++) {
                            int[][] monochromePalette =
                                    patternTableSelector == 0 ?
                                            (ggvm.getBackgroundPatternTableAddress() == 0 ? bgMonochromePalette : sprMonochromePalette) :
                                            ggvm.getSpritePatternTableAddress() == 0 ? bgMonochromePalette : sprMonochromePalette;
                            int pixel = ggvm.getChrPixel(patternTableSelector * 256 + row * 16 + column, x, y);
                            for(int attribute = 0; attribute < 4; attribute++) {
                                //Pattern table X offset in pixels is the attribute times the width of the pattern table, plus
                                //the current column within the pattern table * 8
                                int patternTableXOffsetInPixels = attribute * 128 + column * 8;
                                int patternTableYOffsetInPixels = patternTableSelector * 128 + row * 8;
                                patternTablePixmap.drawPixel(7 - x + patternTableXOffsetInPixels, y + patternTableYOffsetInPixels, monochromePalette[attribute][pixel]);
                            }
                        }
                    }
                }
            }
        }
        patternTableTexture.draw(patternTablePixmap, 0, 0);
    }

    private void fixBleeding(TextureRegion textureRegion) {
        float fix = 0.01f;
        float x = textureRegion.getRegionX();
        float y = textureRegion.getRegionY();
        float width = textureRegion.getRegionWidth();
        float height = textureRegion.getRegionHeight();
        float invTexWidth = 1f / textureRegion.getTexture().getWidth();
        float invTexHeight = 1f / textureRegion.getTexture().getHeight();
        textureRegion.setRegion((x + fix) * invTexWidth, (y + fix) * invTexHeight, (x + width - fix) * invTexWidth, (y + height - fix) * invTexHeight); // Trims
    }
}

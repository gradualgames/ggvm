package com.gradualgames.manager.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.OnGeneratePatternTableListener;

/**
 * Created by derek on 6/10/2017.
 *
 * Base class for pattern table reading, writing, and bankswitching. The base
 * class assumes no chr-rom or ram bankswitching is being performed. In other words,
 * it just assumes the normal 8kb chr-rom or chr-ram is available.
 */
public class PatternTableManager implements OnGeneratePatternTableListener {

    protected GGVm ggvm;
    protected Pixmap patternPixmap;
    protected Pixmap patternTablePixmap;
    protected Texture patternTableTexture;
    protected Sprite[][] patternTableSprites;
    protected int[] monochromePalette = new int[4];

    public PatternTableManager(GGVm ggvm) {
        this.ggvm = ggvm;
        initialize();
    }

    /**
     * Initializes the pattern table textures and pixmap.
     */
    public void initialize() {
        //Allocate a pixmap big enough to accommodate both pattern tables.
        patternTablePixmap = new Pixmap(128, 256, Pixmap.Format.RGBA8888);
        //Set blending to none so we can rewrite the pixmap and draw it to the
        //pattern table texture when graphics are regenerated.
        patternTablePixmap.setBlending(Pixmap.Blending.None);

        //Allocate a pixmap the size of one tile for live CHR-RAM updates.
        patternPixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        patternPixmap.setBlending(Pixmap.Blending.None);

        patternTableTexture = new Texture(patternTablePixmap, false);
        TextureRegion[][] textureRegions = TextureRegion.split(patternTableTexture, 8, 8);
        patternTableSprites = new Sprite[32][16];
        for(int row = 0; row < 32; row++) {
            for(int column = 0; column < 16; column++) {
                TextureRegion textureRegion = textureRegions[row][column];
                patternTableSprites[row][column] = new Sprite(textureRegion);
            }
        }
        initializeMonochromePalette();
    }

    /**
     * Retrieves a sprite from the sprites generated from the pattern table texture.
     * @param patternTable Which pattern table to use (0 or 1)
     * @param row The row of the pattern (0 to 15)
     * @param column The column of the pattern (0 to 15)
     * @return
     */
    public Sprite getSprite(int patternTable, int row, int column) {
        return patternTableSprites[patternTable * 16 + row][column];
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
     * @param transparentColor
     * @return
     */
    protected int pixelToShaderPixel(int pixelValue, boolean transparentColor) {
        float r = (((float) pixelValue) * (.25f / 4f)) + (.25f / 8f);
        return Color.rgba8888(r / 2, 0, 0, transparentColor ? 0f : 1f);
    }

    /**
     * Initializes monochrome palette used when generating textures from chr data.
     * GGVm palettes consist of 4 sets of 4 colors. Each set of 4 is considered an
     * attribute. The g component of every pixel is set up to pick an attribute by
     * setting it to the values 0, .25, .5 and .75, to be used as part of a u,v coordinate
     * by the shader to pick the correct color. The r component consists of further
     * subdivisions of 4 in increments of .25/4, added to the g component to pick the
     * correct color from the background or sprite palette. The b component picks the offset
     * within the palette lookup table, either 0 or .5 to pick bg or sprites.
     */
    protected void initializeMonochromePalette() {
        Gdx.app.log(getClass().getSimpleName(), "initializeMonochromePalette()");
        for (int pixelValue = 0; pixelValue <= 3; pixelValue++) {
            monochromePalette[pixelValue] = pixelToShaderPixel(pixelValue, pixelValue == 0 ? true : false);
        }
    }

    /**
     * Callback from ggvm which tells the application to generate tile graphics based
     * on data in ggvm.
     */
    @Override
    public void onGeneratePatternTable() {
        Gdx.app.log(getClass().getSimpleName(), "onGeneratePatternTable()");
        generateSpritesForPatternTable();
    }

    /**
     * Callback from ggvm which tells the application to generate a single pattern
     * table tile.
     */
    @Override
    public void onGeneratePattern(int patternAddress) {
        int patternIndex = patternAddress >> 4;
        int patternTableSelector = patternIndex >> 8;
        patternIndex &= 0xff;
        int indexRow = patternIndex >> 4;
        int indexColumn = patternIndex & 0x0f;
        int patternTableXOffsetInPixels = indexColumn * 8;
        int patternTableYOffsetInPixels = patternTableSelector * 128 + indexRow * 8;

        //Iterate over current tile in pixel units
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int pixel = ggvm.getChrPixel(patternTableSelector * 256 + indexRow * 16 + indexColumn, x, y);
                //Pattern table X offset in pixels is the attribute times the width of the pattern table, plus
                //the current column within the pattern table * 8
                patternPixmap.drawPixel(7 - x, y, monochromePalette[pixel]);
            }
        }
        patternTableTexture.draw(patternPixmap, patternTableXOffsetInPixels, patternTableYOffsetInPixels);
    }

    /**
     * Generates textures and sprites based on pattern table data in ggvm.
     */
    protected void generateSpritesForPatternTable() {
        //Iterate over current pattern table in tile units
        for(int patternTableSelector = 0; patternTableSelector < 2; patternTableSelector++) {
            for(int row = 0; row < 16; row++) {
                for(int column = 0; column < 16; column++) {
                    int patternTableXOffsetInPixels = column * 8;
                    int patternTableYOffsetInPixels = patternTableSelector * 128 + row * 8;
                    //Iterate over current tile in pixel units
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int pixel = ggvm.getChrPixel(patternTableSelector * 256 + row * 16 + column, x, y);
                            patternTablePixmap.drawPixel(7 - x + patternTableXOffsetInPixels, y + patternTableYOffsetInPixels, monochromePalette[pixel]);
                        }
                    }
                }
            }
        }
        patternTableTexture.draw(patternTablePixmap, 0, 0);
    }
}

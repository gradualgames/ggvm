package com.gradualgames.manager.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.UnromSwitchboard;

/**
 * Created by derek on 6/10/2017.
 *
 * This pattern table manager supports Mapper30 and allows for storing up to
 * 4, 8kb chr-ram banks at once in a single texture. It works with a Mapper30ControlRegister
 * on the cpu bus to determine which chr-ram bank is currently swapped in, which
 * changes which sprites will be read from getSprite, and also which parts of the texture
 * will be written when writes are detected to chr-ram.
 */
public class ChrRamPatternTableManager extends PatternTableManager {

    UnromSwitchboard unromSwitchboard;

    public ChrRamPatternTableManager(GGVm ggvm) {
        super(ggvm);
        unromSwitchboard = (UnromSwitchboard) ggvm.getReadWriteRange(0x8000);
    }

    @Override
    public void initialize() {
        //Allocate a pixmap big enough to accommodate four banks of pattern
        //tables (bg and spr each for all four banks)
        patternTablePixmap = new Pixmap(128, 1024, Pixmap.Format.RGBA8888);
        //Set blending to none so we can rewrite the pixmap and draw it to the
        //pattern table texture when graphics are regenerated.
        patternTablePixmap.setBlending(Pixmap.Blending.None);

        //Allocate a pixmap the size of one tile for live CHR-RAM updates.
        patternPixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        patternPixmap.setBlending(Pixmap.Blending.None);

        patternTableTexture = new Texture(patternTablePixmap, false);
        TextureRegion[][] textureRegions = TextureRegion.split(patternTableTexture, 8, 8);
        patternTableSprites = new Sprite[128][16];
        for(int row = 0; row < 128; row++) {
            for(int column = 0; column < 16; column++) {
                TextureRegion textureRegion = textureRegions[row][column];
                patternTableSprites[row][column] = new Sprite(textureRegion);
            }
        }
        initializeMonochromePalette();
    }

    @Override
    public Sprite getSprite(int patternTable, int row, int column) {
        int currentChr = unromSwitchboard.getCurrentChr();
        return patternTableSprites[patternTable * 16 + row + currentChr * 32][column];
    }

    /**
     * Callback from ggvm which tells the application to generate a single pattern
     * table tile.
     */
    @Override
    public void onGeneratePattern(int patternAddress) {
        int currentChr = unromSwitchboard.getCurrentChr();
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
        patternTableTexture.draw(patternPixmap, patternTableXOffsetInPixels, patternTableYOffsetInPixels + currentChr * 256);
    }

    /**
     * Generates textures and sprites based on pattern table data in ggvm.
     */
    protected void generateSpritesForPatternTable() {
        int currentChr = unromSwitchboard.getCurrentChr();
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
                            patternTablePixmap.drawPixel(7 - x + patternTableXOffsetInPixels, y + patternTableYOffsetInPixels + currentChr * 256, monochromePalette[pixel]);
                        }
                    }
                }
            }
        }
        patternTableTexture.draw(patternTablePixmap, 0, 0);
    }
}

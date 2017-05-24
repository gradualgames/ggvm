package com.gradualgames.manager.render;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.BusListener;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.Ppu;
import com.gradualgames.manager.rastereffect.RasterEffectManager;

/**
 * Created by derek on 1/7/2017.
 *
 * This class draws the background using nametables at $2000 and $2800. See
 * RenderManager for a high level description of how backgrounds are drawn.
 */
public class HorizontalMirroringRenderManager extends RenderManager implements BusListener {

    private boolean sprite0HitStatusBarEnabled = false;

    private boolean sprite0HitStatusBarOn = false;

    public HorizontalMirroringRenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager) {
        super(ggvm, rasterEffectManager);
    }

    public HorizontalMirroringRenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager, boolean sprite0HitStatusBarEnabled) {
        this(ggvm, rasterEffectManager);
        this.sprite0HitStatusBarEnabled = sprite0HitStatusBarEnabled;
        if (this.sprite0HitStatusBarEnabled) {
            ggvm.installVirtualRegister(0x5500, this);
        }
    }

    @Override
    public void drawNametable(GGVm ggvm, SpriteBatch spriteBatch) {
        if (!sprite0HitStatusBarEnabled) {
            drawNametable(ggvm, spriteBatch, ggvm.getScrollX(), ggvm.getScrollY(), 0f, 240f);
        } else {
            if (sprite0HitStatusBarOn) {
                drawNametable(ggvm, spriteBatch, 0, 0, 0, ggvm.getSpriteY(0));
                drawNametable(ggvm, spriteBatch, ggvm.getScrollX(), ggvm.getScrollY(), ggvm.getSpriteY(0), 240f);
            } else {
                drawNametable(ggvm, spriteBatch, ggvm.getScrollX(), ggvm.getScrollY(), 0f, 240f);
            }
        }
    }

    private void drawNametable(GGVm ggvm, SpriteBatch spriteBatch, int scrollX, int scrollY, float splitYStart, float splitYEnd) {
        int patternTableOffset = ggvm.getBackgroundPatternTableAddress() == 0 ? 0 : 1;

        int coarseScrollX = scrollX >> 3;
        int coarseScrollY = scrollY >> 3;
        int fineScrollX = scrollX & 7;
        int fineScrollY = scrollY & 7;

        int nameTableX = coarseScrollX;
        int nameTableY = coarseScrollY;
        int actualNameTableX;
        int actualNameTableY;
        int screenX = 0;
        int screenY = 8;
        int nameTableRowCount = fineScrollY == 0 ? 30 : 31;
        int nameTableColumnCount = 33;
        int nameTable;

        while (nameTableColumnCount > 0) {
            screenY = 0;
            nameTableRowCount = fineScrollY == 0 ? 30 : 31;
            nameTableY = coarseScrollY;
            nameTable = (ggvm.getNametableAddress() == Ppu.NAME_TABLE_0_BASE_ADDRESS) ? 0 : 1;
            while (nameTableRowCount > 0) {
                actualNameTableX = nameTableX % 32;
                actualNameTableY = nameTableY % 30;
                int whichNameTable = nameTable ^ (nameTableY / 30);

                int nameTableAddress = whichNameTable == 1 ? Ppu.NAME_TABLE_2_BASE_ADDRESS : Ppu.NAME_TABLE_0_BASE_ADDRESS;
                int attributeTableAddress = whichNameTable == 1 ? Ppu.ATTRIBUTE_TABLE_2_BASE_ADDRESS : Ppu.ATTRIBUTE_TABLE_0_BASE_ADDRESS;

                int index = ggvm.getNametableTile(nameTableAddress, actualNameTableX, actualNameTableY);
                int attribute = ggvm.getAttributeForNametableTile(attributeTableAddress, actualNameTableX, actualNameTableY);

                int indexRow = index >> 4;
                int indexColumn = index & 0x0f;
                Sprite sprite = patternTableSprites[patternTableOffset * 16 + indexRow][indexColumn];
                sprite.setColor(0, attributes[attribute], 0, 0);
                if (screenY >= splitYStart && screenY < splitYEnd) {
                    sprite.setPosition(screenX - fineScrollX, 232 - screenY + fineScrollY);
                    sprite.draw(spriteBatch);
                }
                screenY += 8;
                nameTableY++;
                nameTableRowCount--;
            }
            screenX += 8;
            nameTableX++;
            nameTableColumnCount--;
        }
    }

    @Override
    public void onRead(int address) {
        //Intentionally left blank.
    }

    @Override
    public void onWrite(int address, byte value) {
        if (address == 0x5500) {
            if (value != 0) {
                sprite0HitStatusBarOn = true;
            } else {
                sprite0HitStatusBarOn = false;
            }
        }
    }
}

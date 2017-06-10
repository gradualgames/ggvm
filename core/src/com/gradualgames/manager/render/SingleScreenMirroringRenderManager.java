package com.gradualgames.manager.render;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.Ppu;
import com.gradualgames.manager.rastereffect.RasterEffectManager;

/**
 * Created by derek on 6/10/2017.
 *
 * Single-screen mirroring render manager. This just reads from the first nametable
 * and wraps in all directions, so is somewhat simpler in implementation than the horizontal
 * or vertical mirroring render managers.
 */
public class SingleScreenMirroringRenderManager extends RenderManager {
    public SingleScreenMirroringRenderManager(GGVm ggvm, PatternTableManager patternTableManager, RasterEffectManager rasterEffectManager, boolean statusBarEnabled) {
        super(ggvm, patternTableManager, rasterEffectManager);
    }

    @Override
    protected void drawNametable(GGVm ggvm, SpriteBatch spriteBatch) {
        drawNametable(ggvm, spriteBatch, ggvm.getScrollX(), ggvm.getScrollY(), 0f, 240f);
    }

    private void drawNametable(GGVm ggvm, SpriteBatch spriteBatch, int scrollX, int scrollY, float splitYStart, float splitYEnd) {
        int patternTable = ggvm.getBackgroundPatternTableAddress() == 0 ? 0 : 1;

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

                int nameTableAddress = nameTable == 1 ? Ppu.NAME_TABLE_1_BASE_ADDRESS : Ppu.NAME_TABLE_0_BASE_ADDRESS;
                int attributeTableAddress = nameTableAddress == 1 ? Ppu.ATTRIBUTE_TABLE_1_BASE_ADDRESS : Ppu.ATTRIBUTE_TABLE_0_BASE_ADDRESS;

                int index = ggvm.getNametableTile(nameTableAddress, actualNameTableX, actualNameTableY);
                int attribute = ggvm.getAttributeForNametableTile(attributeTableAddress, actualNameTableX, actualNameTableY);

                int indexRow = index >> 4;
                int indexColumn = index & 0x0f;
                Sprite sprite = patternTableManager.getSprite(patternTable, indexRow, indexColumn);//patternTableSprites[patternTableOffset * 16 + indexRow][indexColumn];
                sprite.setColor(0, attributes[attribute], 0, 0);
                //if (!GGVmRegisterStatusBar.isSprite0HitStatusBarEnabled() || (screenY >= splitYStart && screenY < splitYEnd)) {
                    sprite.setPosition(screenX - fineScrollX, 232 - screenY + fineScrollY);
                    sprite.draw(spriteBatch);
                //}
                screenY += 8;
                nameTableY++;
                nameTableRowCount--;
            }
            screenX += 8;
            nameTableX++;
            nameTableColumnCount--;
        }
    }
}

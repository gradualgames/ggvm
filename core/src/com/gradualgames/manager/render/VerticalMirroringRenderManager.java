package com.gradualgames.manager.render;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.Ppu;
import com.gradualgames.manager.rastereffect.RasterEffectManager;

/**
 * Created by derek on 1/7/2017.
 *
 * This class draws the background using nametables at $2000 and $2400. See
 * RenderManager for a high level description of how backgrounds are drawn.
 */
public class VerticalMirroringRenderManager extends RenderManager {

    public VerticalMirroringRenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager) {
        super(ggvm, rasterEffectManager);
    }

    public void drawNametable(GGVm ggvm, SpriteBatch spriteBatch) {
        int patternTableOffset = ggvm.getBackgroundPatternTableAddress() == 0 ? 0 : 1;

        int scrollX = ggvm.getScrollX();
        int scrollY = ggvm.getScrollY();
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
        int nameTableColumnCount;
        int nameTable;

        while (nameTableRowCount > 0) {
            screenX = 0;
            nameTableColumnCount = fineScrollX == 0 ? 32 : 33;
            nameTableX = coarseScrollX;
            nameTable = (ggvm.getNametableAddress() == Ppu.NAME_TABLE_0_BASE_ADDRESS) ? 0 : 1;
            while (nameTableColumnCount > 0) {
                actualNameTableX = nameTableX % 32;
                actualNameTableY = nameTableY % 30;
                int whichNameTable = nameTable ^ ((nameTableX >> 5) & 1);

                int nameTableAddress = whichNameTable == 1 ? Ppu.NAME_TABLE_1_BASE_ADDRESS : Ppu.NAME_TABLE_0_BASE_ADDRESS;
                int attributeTableAddress = whichNameTable == 1 ? Ppu.ATTRIBUTE_TABLE_1_BASE_ADDRESS : Ppu.ATTRIBUTE_TABLE_0_BASE_ADDRESS;

                int index = ggvm.getNametableTile(nameTableAddress, actualNameTableX, actualNameTableY);
                int attribute = ggvm.getAttributeForNametableTile(attributeTableAddress, actualNameTableX, actualNameTableY);

                int indexRow = index >> 4;
                int indexColumn = index & 0x0f;
                Sprite sprite = patternTableSprites[patternTableOffset * 16 + indexRow][indexColumn];
                sprite.setColor(0, attributes[attribute], 0, 0);
                sprite.setPosition(screenX - fineScrollX, 240 - screenY + fineScrollY);
                sprite.draw(spriteBatch);
                screenX += 8;
                nameTableX++;
                nameTableColumnCount--;
            }
            screenY += 8;
            nameTableY++;
            nameTableRowCount--;
        }
    }
}

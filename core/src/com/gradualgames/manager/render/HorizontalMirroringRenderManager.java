package com.gradualgames.manager.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.ggvm.Ppu;
import com.gradualgames.manager.rastereffect.RasterEffectManager;

/**
 * Created by derek on 1/7/2017.
 *
 * This class draws the background using nametables at $2000 and $2800. See
 * RenderManager for a high level description of how backgrounds are drawn.
 */
public class HorizontalMirroringRenderManager extends RenderManager {

    public HorizontalMirroringRenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager) {
        super(ggvm, rasterEffectManager);
    }

    @Override
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
        int nameTableColumnCount = 32;
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
                patternTableSprites[patternTableOffset * 16 + indexRow][attribute * 16 + indexColumn].setPosition(screenX - fineScrollX, 232 - screenY + fineScrollY);
                patternTableSprites[patternTableOffset * 16 + indexRow][attribute * 16 + indexColumn].draw(spriteBatch);
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

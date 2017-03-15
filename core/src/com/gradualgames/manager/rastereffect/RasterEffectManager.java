package com.gradualgames.manager.rastereffect;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by derek on 10/1/2016.
 *
 * Intended to handle applying raster effects specific to a given game. In
 * most cases this will involve drawing a scroll-hiding bar, or applying a
 * shader at the tail end of a sprite batch drawNametable. Note: This has
 * not been put to use in games with more advanced raster effects such as
 * split screens. In all likelihood, a wholly new RenderManager extension
 * may be necessary in these cases.
 */
public interface RasterEffectManager {

    void render(SpriteBatch spriteBatch);

}

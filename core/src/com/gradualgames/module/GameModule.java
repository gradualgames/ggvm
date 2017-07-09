package com.gradualgames.module;

import com.gradualgames.ggvm.Cartridge;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.manager.nmi.NmiSafeFunctor;
import com.gradualgames.manager.render.PatternTableManager;
import com.gradualgames.manager.render.RenderManager;
import com.gradualgames.manager.rastereffect.RasterEffectManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;

/**
 * Created by derek on 10/1/2016.
 *
 * A game module provides the rom file name, font file names,
 * icon file name, and then game-specific adapters for rendering,
 * raster effects, soundtrack, and an nmi safety functor. It also
 * provides the cartridge object. Depending on the requirements of
 * a given game developer, the Cartridge object can be configured
 * from the game module instead of relying on the iNES header, allowing
 * game authors to strip the iNES header from their rom to discourage
 * casual hackers.
 */
public interface GameModule {

    String provideTitle();

    String provideFileName();

    String provideFontBitmapFileName();

    String provideFontFileName();

    String provideIconFileName();

    String provideDpadFileName();

    String provideSSFileName();

    String provideABFileName();

    Cartridge provideCartridge(byte[] bytes);

    RenderManager provideRenderManager(GGVm ggvm, PatternTableManager patternTableManager, RasterEffectManager rasterEffectManager);

    PatternTableManager providePatternTableManager(GGVm ggvm);

    RasterEffectManager provideRasterEffectManager(GGVm ggvm);

    SoundtrackManager provideSoundtrackManager(GGVm ggvm);

    NmiSafeFunctor provideNmiSafeFunctor();
}

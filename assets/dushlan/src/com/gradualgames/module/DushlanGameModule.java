package com.gradualgames.module;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.Cartridge;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.manager.nmi.NmiSafeFunctor;
import com.gradualgames.manager.render.PatternTableManager;
import com.gradualgames.manager.render.RenderManager;
import com.gradualgames.manager.rastereffect.RasterEffectManager;
import com.gradualgames.manager.soundtrack.DushlanSoundtrackManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;
import com.gradualgames.manager.render.VerticalMirroringRenderManager;

/**
 * Created by derek on 1/7/2017.
 */
public class DushlanGameModule implements GameModule {
    @Override
    public String provideTitle() {
        return "Dushlan";
    }

    @Override
    public String provideFileName() {
        return "dushlan/dushlan.nes";
    }

    @Override
    public String provideFontBitmapFileName() {
        return "font/gameover.png";
    }

    @Override
    public String provideFontFileName() {
        return "font/gameover.fnt";
    }

    @Override
    public String provideIconFileName() {
        return null;
    }

    @Override
    public String provideDpadFileName() { return "gamepad/dpad.png"; }

    @Override
    public String provideSSFileName() { return "gamepad/ss.png"; }

    @Override
    public String provideABFileName() { return "gamepad/ab.png"; }

    @Override
    public Cartridge provideCartridge(byte[] bytes) {
        return new Cartridge(bytes);
    }

    public PatternTableManager providePatternTableManager(GGVm ggvm) {
        return new PatternTableManager(ggvm);
    }

    @Override
    public RenderManager provideRenderManager(GGVm ggvm, PatternTableManager patternTableManager, RasterEffectManager rasterEffectManager) {
        return new VerticalMirroringRenderManager(ggvm, patternTableManager, rasterEffectManager, false);
    }

    @Override
    public RasterEffectManager provideRasterEffectManager(GGVm ggvm) {
        return new RasterEffectManager() {
            @Override
            public void render(SpriteBatch spriteBatch) {

            }
        };
    }

    @Override
    public SoundtrackManager provideSoundtrackManager(GGVm ggvm) {
        return new DushlanSoundtrackManager(ggvm);
    }

    @Override
    public NmiSafeFunctor provideNmiSafeFunctor() {
        return new NmiSafeFunctor() {
            @Override
            public boolean isPcInSafeRange(int pc) {
                return true;
            }
        };
    }
}

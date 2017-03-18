package com.gradualgames.module;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gradualgames.ggvm.Cartridge;
import com.gradualgames.ggvm.GGVm;
import com.gradualgames.manager.nmi.NmiSafeFunctor;
import com.gradualgames.manager.rastereffect.RasterEffectManager;
import com.gradualgames.manager.render.RenderManager;
import com.gradualgames.manager.render.VerticalMirroringRenderManager;
import com.gradualgames.manager.soundtrack.SoundtrackManager;

/**
 * Created by derek on 1/17/2017.
 */
public class ChrRamTestGameModule implements GameModule {
    @Override
    public String provideTitle() {
        return "CHR RAM Streaming Test";
    }

    @Override
    public String provideFileName() {
        return "chrramtest/chrram_stream_test.nes";
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
    public Cartridge provideCartridge(byte[] bytes) {
        return new Cartridge(bytes);
    }

    @Override
    public RenderManager provideRenderManager(GGVm ggvm, RasterEffectManager rasterEffectManager) {
        return new VerticalMirroringRenderManager(ggvm, rasterEffectManager);
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
        return new SoundtrackManager("chrramtest", ggvm) {
            @Override
            protected void handleOnRead(int address) {

            }

            @Override
            protected void handleOnWrite(int address, byte value) {

            }
        };
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

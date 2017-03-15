package com.gradualgames.ggvm;

/**
 * Created by derek on 9/10/2016.
 *
 * This interface tells the listener to generate tiles from chr data on the
 * ppu bus. See the GGVm object for how it inspects the PPu to determine when
 * to generate this event. Also see GGVmApplication, and particularly
 * RenderManager to see how ppu data is transformed into actual textures for
 * use in a modern gpu.
 */
public interface OnGenerateGraphicsListener {

    void onGenerateGraphics();
}

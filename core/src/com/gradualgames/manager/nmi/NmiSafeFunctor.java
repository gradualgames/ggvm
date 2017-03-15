package com.gradualgames.manager.nmi;

/**
 * Created by derek on 2/5/2017.
 *
 * This interface defines a single method for determining if the
 * program counter is in a safe range for firing nmi. This can be used
 * for games that are not totally ironed out with respect to timing
 * issues with nmi but which already work well on real NES hardware and
 * emulators due to being tuned to precision timing. Since ggvm cannot
 * provide precision timing, we can protect nmi by disallowing it except
 * where the game is performing an nmi wait. In practice this has shown
 * to be very effective, and indeed necessary since GGVm was never
 * designed to provide cycle accurate timing.
 */
public interface NmiSafeFunctor {

    boolean isPcInSafeRange(int pc);

}

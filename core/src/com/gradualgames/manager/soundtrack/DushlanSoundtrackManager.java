package com.gradualgames.manager.soundtrack;

import com.gradualgames.ggvm.GGVm;

/**
 * Created by derek on 1/24/2017.
 */
public class DushlanSoundtrackManager extends SoundtrackManager {

    private static final int play_song = 0xa4f9;
    private static final int play_sfx = 0xa61b;
    private static final int nmi = 0x811a;
    private static final int sound_param_byte_0 = 0xb;

    private String loopingSongPath = "";

    private enum DushlanSongIndex {
        song_index_Teuthida,
        song_index_Title,
        song_index_Slow_Theme_A,
        song_index_Fast_Theme_A,
        song_index_Slow_Theme_B,
        song_index_Fast_Theme_B,
        song_index_Slow_Theme_C,
        song_index_Fast_Theme_C,
        song_index_Mission_Failed,
        song_index_Mission_Complete_1,
        song_index_Mission_Complete_2
    }

    private enum DushlanSfxIndex {
        sfx_index_sfx_Spin,
        sfx_index_sfx_Drop,
        sfx_index_sfx_Lock_or_Move,
        sfx_index_sfx_Clearx1,
        sfx_index_sfx_Clearx2,
        sfx_index_sfx_Clearx3,
        sfx_index_sfx_NotTetris,
        sfx_index_sfx_Ghost,
        sfx_index_sfx_Store,
        sfx_index_sfx_Boom,
        sfx_index_sfx_GoodClear_or_Defuse,
        sfx_index_sfx_Appear
    }

    public DushlanSoundtrackManager(GGVm ggvm) {
        super("dushlan", ggvm);
        ggvm.installBusEventGenerator(play_song, 1, this);
        ggvm.installBusEventGenerator(play_sfx, 1, this);
    }

    @Override
    protected void handleOnRead(int address) {
        int soundParamByte0 = ggvm.readUnsignedByteAsInt(sound_param_byte_0);
        switch(address) {
            case play_song:
                switch(DushlanSongIndex.values()[soundParamByte0]) {
                    case song_index_Teuthida:
                        playSong("dushlan/music/teuthida_splash.ogg", false);
                        break;
                    case song_index_Title:
                        playSong("dushlan/music/title.ogg", true);
                        break;
                    case song_index_Slow_Theme_A:
                        playSong("dushlan/music/a_slow.ogg", true);
                        break;
                    case song_index_Fast_Theme_A:
                        playSong("dushlan/music/a_fast.ogg", true);
                        break;
                    case song_index_Slow_Theme_B:
                        ggvm.installBusEventGenerator(nmi, 1, this);
                        loopingSongPath = "dushlan/music/b_slow_loop.ogg";
                        playSong("dushlan/music/b_slow_intro.ogg", false);
                        break;
                    case song_index_Fast_Theme_B:
                        ggvm.installBusEventGenerator(nmi, 1, this);
                        loopingSongPath = "dushlan/music/b_fast_loop.ogg";
                        playSong("dushlan/music/b_fast_intro.ogg", false);
                        break;
                    case song_index_Slow_Theme_C:
                        playSong("dushlan/music/c_slow.ogg", true);
                        break;
                    case song_index_Fast_Theme_C:
                        playSong("dushlan/music/c_fast.ogg", true);
                        break;
                    case song_index_Mission_Failed:
                        playSong("dushlan/music/mission_failed.ogg", true);
                        break;
                    case song_index_Mission_Complete_1:
                        playSong("dushlan/music/mission_complete_1.ogg", true);
                        break;
                    case song_index_Mission_Complete_2:
                        playSong("dushlan/music/mission_complete_2.ogg", true);
                        break;
                }
                break;
            case play_sfx:
                switch(DushlanSfxIndex.values()[soundParamByte0]) {
                    case sfx_index_sfx_Spin:
                        playSfx("dushlan/sfx/sfx_spin.ogg");
                        break;
                    case sfx_index_sfx_Drop:
                        playSfx("dushlan/sfx/sfx_drop.ogg");
                        break;
                    case sfx_index_sfx_Lock_or_Move:
                        playSfx("dushlan/sfx/sfx_lock_or_move.ogg");
                        break;
                    case sfx_index_sfx_Clearx1:
                        playSfx("dushlan/sfx/sfx_clearx1.ogg");
                        break;
                    case sfx_index_sfx_Clearx2:
                        playSfx("dushlan/sfx/sfx_clearx2.ogg");
                        break;
                    case sfx_index_sfx_Clearx3:
                        playSfx("dushlan/sfx/sfx_clearx3.ogg");
                        break;
                    case sfx_index_sfx_NotTetris:
                        playSfx("dushlan/sfx/sfx_nottetris.ogg");
                        break;
                    case sfx_index_sfx_Ghost:
                        playSfx("dushlan/sfx/sfx_ghost.ogg");
                        break;
                    case sfx_index_sfx_Store:
                        playSfx("dushlan/sfx/sfx_store.ogg");
                        break;
                    case sfx_index_sfx_Boom:
                        playSfx("dushlan/sfx/sfx_boom.ogg");
                        break;
                    case sfx_index_sfx_GoodClear_or_Defuse:
                        playSfx("dushlan/sfx/sfx_good_clear_or_defuse.ogg");
                        break;
                    case sfx_index_sfx_Appear:
                        playSfx("dushlan/sfx/sfx_appear.ogg");
                        break;
                }
                break;
            case nmi:
                if (currentSong != null && !currentSong.isPlaying()) {
                    ggvm.uninstallBusEventGenerator(nmi, 1);
                    playSongNoStop(loopingSongPath, true);
                }
                break;
        }
    }

    @Override
    protected void handleOnWrite(int address, byte value) {

    }
}

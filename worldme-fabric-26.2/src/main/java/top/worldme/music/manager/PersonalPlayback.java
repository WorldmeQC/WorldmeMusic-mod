package top.worldme.music.manager;

import top.worldme.music.WorldmeMusic;
import top.worldme.music.WorldmeMusicPlayer;
import top.worldme.music.net.ModNetwork;
import top.worldme.music.state.MusicState;

/**
 * 个人歌单自动续播管理器（客户端）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class PersonalPlayback {

    private static final long SPURIOUS_END_WINDOW_MS = 2000L;
    private static final int MAX_CONSECUTIVE_FAILS = 3;
    private static final long SWITCH_TIMEOUT_MS = 10000L;

    private static long lastPlayStartedAt = 0L;
    private static int consecutiveFails = 0;
    private static volatile boolean switching = false;
    private static volatile long switchingStartedAt = 0L;

    private PersonalPlayback() {
    }

    /** 服务端已下发个人播放，记录起始时间。 */
    public static void onPrivatePlayStarted() {
        lastPlayStartedAt = System.currentTimeMillis();
        consecutiveFails = 0;
        switching = false;
    }

    /** 是否正在等待服务端返回下一首。 */
    public static boolean isSwitching() {
        if (!switching) {
            return false;
        }
        if (System.currentTimeMillis() - switchingStartedAt > SWITCH_TIMEOUT_MS) {
            switching = false;
            return false;
        }
        return true;
    }

    public static long getSwitchingElapsedMs() {
        return System.currentTimeMillis() - switchingStartedAt;
    }

    private static void requestPlay(long songId) {
        switching = true;
        switchingStartedAt = System.currentTimeMillis();
        ModNetwork.sendPlayPrivateReq(songId);
    }

    /** 曲目自然结束（已切到客户端线程）。 */
    public static void onTrackEnded() {
        if (WorldmeMusic.getPlayMode() != WorldmeMusic.MODE_PERSONAL) {
            return;
        }
        if (System.currentTimeMillis() - lastPlayStartedAt < SPURIOUS_END_WINDOW_MS) {
            return;
        }
        playNext();
    }

    /** 服务端个人播放失败。 */
    public static void onPlayFail() {
        if (WorldmeMusic.getPlayMode() != WorldmeMusic.MODE_PERSONAL) {
            return;
        }
        if (++consecutiveFails > MAX_CONSECUTIVE_FAILS) {
            consecutiveFails = 0;
            switching = false;
            MusicState.get().resetPersonalPlayback();
            return;
        }
        playNext();
    }

    /**
     * 手动切到下一首；已是最后一首则停止并清除播放标记。
     */
    public static void skipToNext() {
        if (WorldmeMusic.getPlayMode() != WorldmeMusic.MODE_PERSONAL || isSwitching()) {
            return;
        }
        MusicState state = MusicState.get();
        MusicState.Song next = state.advancePersonal();
        if (next == null) {
            state.clearLyrics();
            reset();
            WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
            if (player != null) {
                player.stopAsync();
            }
            return;
        }
        state.clearLyrics();
        requestPlay(next.id);
    }

    private static void playNext() {
        MusicState.Song next = MusicState.get().advancePersonal();
        if (next == null) {
            consecutiveFails = 0;
            switching = false;
            return;
        }
        requestPlay(next.id);
    }

    /** 停止或切换模式时重置。 */
    public static void reset() {
        consecutiveFails = 0;
        lastPlayStartedAt = 0L;
        switching = false;
    }
}

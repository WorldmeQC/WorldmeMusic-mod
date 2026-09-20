package top.worldme.music.manager;

import top.worldme.music.WorldmeMusic;
import top.worldme.music.WorldmeMusicPlayer;

/**
 * 播放进度缓存：每客户端 tick 刷新一次，供界面与 HUD 渲染读取，
 * 避免在每个渲染帧里重复调用原生 JNI（getPosition/getDuration/getState）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class PlaybackProgress {

    private static volatile long positionMs;
    private static volatile long durationMs;
    private static volatile int state = WorldmeMusicPlayer.STATE_STOPPED;

    private PlaybackProgress() {
    }

    public static void update() {
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player == null) {
            positionMs = 0L;
            durationMs = 0L;
            state = WorldmeMusicPlayer.STATE_STOPPED;
            return;
        }
        state = player.getState();
        positionMs = Math.max(0L, player.getPosition());
        durationMs = Math.max(0L, player.getDuration());
    }

    public static long getPositionMs() {
        return positionMs;
    }

    public static long getDurationMs() {
        return durationMs;
    }

    public static int getState() {
        return state;
    }

    public static boolean isPlaying() {
        return state == WorldmeMusicPlayer.STATE_PLAYING;
    }

    public static float getProgress() {
        return durationMs > 0 ? Math.min(1f, (float) positionMs / (float) durationMs) : 0f;
    }
}

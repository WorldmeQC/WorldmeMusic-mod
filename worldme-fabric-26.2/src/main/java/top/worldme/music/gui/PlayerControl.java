package top.worldme.music.gui;

import net.minecraft.network.chat.Component;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.WorldmeMusicPlayer;
import top.worldme.music.manager.PersonalPlayback;
import top.worldme.music.manager.PlaybackProgress;
import top.worldme.music.state.MusicState;

import java.util.List;

/**
 * 播放控制共享逻辑（主界面与新播放界面复用）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class PlayerControl {

    private PlayerControl() {
    }

    /** 当前展示的歌曲信息。 */
    public static final class NowPlaying {
        public final String name;
        public final String artist;
        public final String coverUrl;

        public NowPlaying(String name, String artist, String coverUrl) {
            this.name = name == null ? "" : name;
            this.artist = artist == null ? "" : artist;
            this.coverUrl = coverUrl == null ? "" : coverUrl;
        }

        public boolean isEmpty() {
            return name.isEmpty();
        }
    }

    public static NowPlaying resolve(MusicState state) {
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            List<MusicState.Song> queue = state.getPersonalQueue();
            int index = state.getCurrentPersonalIndex();
            if (index >= 0 && index < queue.size()) {
                MusicState.Song song = queue.get(index);
                String cover = song.coverUrl.isBlank() ? state.getPersonalCover() : song.coverUrl;
                return new NowPlaying(song.name, song.artist, cover);
            }
            return new NowPlaying("", "", "");
        }
        return new NowPlaying(state.getCurrentName(), state.getCurrentArtist(), state.getCurrentCover());
    }

    public static void togglePlayPause(MusicState state) {
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player == null) {
            return;
        }
        if (PlaybackProgress.isPlaying()) {
            player.pause();
        } else {
            resume(state);
        }
    }

    public static void stop(MusicState state) {
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player == null) {
            return;
        }
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER) {
            state.setLocalStopped(true);
        } else {
            state.resetPersonalPlayback();
        }
        state.clearLyrics();
        PersonalPlayback.reset();
        player.stopAsync();
    }

    public static void resume(MusicState state) {
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player == null) {
            return;
        }
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER) {
            String url = state.getLastServerUrl();
            if (state.isLocalStopped() && !url.isEmpty()) {
                state.setLocalStopped(false);
                player.playAsync(url);
            } else {
                player.resume();
            }
        } else {
            player.resume();
        }
    }

    public static Component buildPlayPauseLabel() {
        return Component.literal(PlaybackProgress.isPlaying() ? "⏸ 暂停" : "▶ 播放");
    }

    /**
     * 第二个按钮：个人模式为「下一首」，全服模式为「停止」。
     */
    public static Component buildSecondaryLabel() {
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            if (PersonalPlayback.isSwitching()) {
                int dots = (int) ((PersonalPlayback.getSwitchingElapsedMs() / 300L) % 3) + 1;
                return Component.literal("切换中" + ".".repeat(dots));
            }
            return Component.literal("⏭ 下一首");
        }
        return Component.literal("⏹ 停止");
    }

    public static void secondaryAction(MusicState state) {
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            PersonalPlayback.skipToNext();
        } else {
            stop(state);
        }
    }

    public static String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }
}

package top.worldme.music.event;

import lombok.extern.log4j.Log4j2;
import top.worldme.music.WorldmeMusic;


/**
 * 发包事件
 *
 * @author 真心
 * @email qgzhenxin@qq.com
 * @since 2023/1/29 22:50
 */
@Log4j2
class PacketEvent {

    public static void onPlay(String data) {
        log.info("Play music from {}", data);
        if (data == null || data.trim().isEmpty()) {
            log.warn("Ignored empty WorldmeMusic play url");
            return;
        }
        if (WorldmeMusic.getSoundManager() == null) {
            log.warn("WorldmeMusic SoundManager is not initialized");
        } else {
            log.info("Stopping vanilla music before WorldmeMusic playback");
            WorldmeMusic.getSoundManager().stop();
        }
        if (WorldmeMusic.getPlayer() == null) {
            log.warn("WorldmeMusic player is not initialized");
            return;
        }
        WorldmeMusic.getPlayer().playAsync(data);
    }

    public static void onPlayPrivate(String data) {
        log.info("Play private music from {}", data);
        if (data == null || data.trim().isEmpty()) {
            log.warn("Ignored empty WorldmeMusic private play url");
            return;
        }
        if (WorldmeMusic.getSoundManager() == null) {
            log.warn("WorldmeMusic SoundManager is not initialized");
        } else {
            log.info("Stopping vanilla music before WorldmeMusic private playback");
            WorldmeMusic.getSoundManager().stop();
        }
        if (WorldmeMusic.getPlayer() == null) {
            log.warn("WorldmeMusic player is not initialized");
            return;
        }
        WorldmeMusic.getPlayer().playAsync(data);
    }

    public static void onStop() {
        log.info("Stop WorldmeMusic playback");
        if (WorldmeMusic.getPlayer() == null) {
            log.warn("WorldmeMusic player is not initialized");
            return;
        }
        WorldmeMusic.getPlayer().stopAsync();
    }

    public static void onLyric(String data) {
        // TODO: 歌词
    }

    public static void onInfo(String data) {
        // TODO: 信息
    }

    public static void onImg(String data) {
        // TODO: 专辑图片
    }
}
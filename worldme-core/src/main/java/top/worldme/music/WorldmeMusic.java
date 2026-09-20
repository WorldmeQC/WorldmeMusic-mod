package top.worldme.music;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import top.worldme.music.manager.SoundManager;


/**
 * WorldmeMusic 主入口
 *
 * @author 真心
 * @email qgzhenxin@qq.com
 * @since 2023/1/28 13:08
 */
@SuppressWarnings({"AlibabaClassNamingShouldBeCamel", "AlibabaConstantFieldShouldBeUpperCase"})
@Log4j2
public class WorldmeMusic {

    /** 全服模式：自动接收服务端全服播放要求。 */
    public static final int MODE_SERVER = 0;
    /** 个人模式：忽略全服播放要求，仅自己点歌自己听。 */
    public static final int MODE_PERSONAL = 1;

    @Getter
    private static WorldmeMusicPlayer player;
    private static boolean shutdownHookRegistered;
    @Getter
    @Setter
    private static SoundManager soundManager;
    @Getter
    private static String version = "2.0.0";

    @Getter
    @Setter
    private static int playMode = MODE_SERVER;

    /** 曲目自然播放结束时的回调（由平台层设置，用于个人歌单自动续播）。 */
    @Getter
    @Setter
    private static Runnable trackEndListener;

    public static void onEnable() {
        if (player != null) {
            player.destroy();
        }
        player = new WorldmeMusicPlayer();
        player.setEventListener(new WorldmeMusicPlayer.EventListener() {
            @Override
            public void onStateChanged(int state) {
                log.info("Worldme native player state changed: {}", state);
            }

            @Override
            public void onTrackEnded() {
                log.info("Worldme native track ended");
                Runnable listener = trackEndListener;
                if (listener != null) {
                    listener.run();
                }
            }

            @Override
            public void onProgress(long positionMs, long durationMs) {
            }

            @Override
            public void onError(String message) {
                log.warn("Worldme native player error: {}", message);
            }

            @Override
            public void onBuffering(boolean buffering) {
                log.info("Worldme native player buffering: {}", buffering);
            }
        });
        registerShutdownHook();
        log.info("Welcome use WorldmeMusic!");
    }

    public static void onDisable() {
        if (player != null) {
            player.destroy();
            player = null;
        }
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(WorldmeMusic::onDisable, "worldme-shutdown"));
        shutdownHookRegistered = true;
    }
}
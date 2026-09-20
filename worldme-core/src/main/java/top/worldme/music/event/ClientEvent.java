package top.worldme.music.event;

import lombok.extern.log4j.Log4j2;
import top.worldme.music.WorldmeMusic;

/**
 * 客户端事件
 *
 * @author 真心
 * @email qgzhenxin@qq.com
 * @since 2023/1/29 22:52
 */
@Log4j2
public class ClientEvent {

    @SuppressWarnings("AlibabaUndefineMagicConstant")
    public static void onPacket(String message) {
        if (message == null) {
            log.warn("Received null WorldmeMusic packet message");
            return;
        }
        log.info("Received WorldmeMusic packet message: {}", message);
        if (message.startsWith("[PlayPriv]")) {
            String data = message.substring("[PlayPriv]".length());
            log.info("Parsed WorldmeMusic private play command: {}", data);
            PacketEvent.onPlayPrivate(data);
        } else if (message.startsWith("[Play]")) {
            String data = message.substring("[Play]".length());
            if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
                log.info("Personal mode enabled, ignored server-wide play: {}", data);
                return;
            }
            log.info("Parsed WorldmeMusic play command: {}", data);
            PacketEvent.onPlay(data);
        } else if ("[Stop]".equals(message)) {
            if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
                log.info("Personal mode enabled, ignored server-wide stop");
                return;
            }
            log.info("Parsed WorldmeMusic stop command");
            PacketEvent.onStop();
        } else {
            // 其它结构化消息（[Queue]/[SearchResult] 等）由客户端层分发处理
            log.debug("Ignored unknown WorldmeMusic packet message: {}", message);
        }
    }

    public static void onDisconnect() {
        log.info("WorldmeMusic client disconnected, stopping native player");
        PacketEvent.onStop();
    }
}
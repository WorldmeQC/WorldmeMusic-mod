package top.worldme.music.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.worldme.music.event.ClientEvent;
import top.worldme.music.state.MusicState;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端下行消息分发器。
 *
 * <p>{@code [Play]/[PlayPriv]/[Stop]} 交给核心 {@link ClientEvent} 处理，
 * {@code [Queue]} / {@code [SearchResult]} 解析后写入 {@link MusicState} 供 GUI 展示。</p>
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class MusicPacketHandler {

    private MusicPacketHandler() {
    }

    public static void handle(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (message.startsWith("[PlayPriv]")) {
            MusicState state = MusicState.get();
            state.setLastServerUrl("");
            state.markPersonalPlayingLatest();
            ClientEvent.onPacket(message);
        } else if (message.startsWith("[Play]")) {
            String url = message.substring("[Play]".length());
            MusicState.get().setLastServerUrl(url);
            MusicState.get().setLocalStopped(false);
            ClientEvent.onPacket(message);
        } else if (message.startsWith("[Queue]")) {
            handleQueue(message.substring("[Queue]".length()));
        } else if (message.startsWith("[SearchResult]")) {
            handleSearchResult(message.substring("[SearchResult]".length()));
        } else {
            ClientEvent.onPacket(message);
        }
    }

    private static void handleQueue(String json) {
        MusicState state = MusicState.get();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            int total = getInt(root, "total", 0);
            String currentName = "";
            String currentArtist = "";
            if (root.has("current") && root.get("current").isJsonObject()) {
                JsonObject current = root.getAsJsonObject("current");
                currentName = getString(current, "name", "");
                currentArtist = getString(current, "artist", "");
            }
            List<MusicState.Song> next = new ArrayList<>();
            if (root.has("next") && root.get("next").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("next")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = element.getAsJsonObject();
                    next.add(new MusicState.Song(0,
                            getString(obj, "name", ""),
                            getString(obj, "artist", ""),
                            "", 0));
                }
            }
            state.setQueue(total, currentName, currentArtist, next);
        } catch (Exception ignored) {
        }
    }

    private static void handleSearchResult(String json) {
        MusicState state = MusicState.get();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String keyword = getString(root, "keyword", "");
            int offset = getInt(root, "offset", 0);
            List<MusicState.Song> songs = new ArrayList<>();
            if (root.has("songs") && root.get("songs").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("songs")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = element.getAsJsonObject();
                    songs.add(new MusicState.Song(
                            getLong(obj, "id", 0),
                            getString(obj, "name", ""),
                            getString(obj, "artist", ""),
                            getString(obj, "album", ""),
                            getLong(obj, "duration", 0)));
                }
            }
            state.setSearchResults(keyword, offset, songs);
        } catch (Exception ignored) {
        }
    }

    private static String getString(JsonObject obj, String key, String def) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return def;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return def;
        }
    }

    private static int getInt(JsonObject obj, String key, int def) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return def;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    private static long getLong(JsonObject obj, String key, long def) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return def;
        }
        try {
            return obj.get(key).getAsLong();
        } catch (Exception e) {
            return def;
        }
    }
}
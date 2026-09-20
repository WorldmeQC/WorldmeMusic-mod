package top.worldme.music.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.worldme.music.PacketTags;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.event.ClientEvent;
import top.worldme.music.gui.CoverTextureCache;
import top.worldme.music.manager.PersonalPlayback;
import top.worldme.music.state.Lyrics;
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
        if (message.startsWith(PacketTags.PLAY_PRIV_FAIL)) {
            PersonalPlayback.onPlayFail();
        } else if (message.startsWith(PacketTags.PLAY_PRIV)) {
            MusicState state = MusicState.get();
            state.setLastServerUrl("");
            state.clearLyrics();
            PersonalPlayback.onPrivatePlayStarted();
            prefetchCover(state.getPersonalCover());
            ClientEvent.onPacket(message);
        } else if (message.startsWith(PacketTags.PLAY)) {
            String url = message.substring(PacketTags.PLAY.length());
            MusicState state = MusicState.get();
            state.setLastServerUrl(url);
            state.setLocalStopped(false);
            state.clearLyrics();
            prefetchCover(state.getCurrentCover());
            ClientEvent.onPacket(message);
        } else if (PacketTags.STOP.equals(message)) {
            MusicState.get().clearLyrics();
            ClientEvent.onPacket(message);
        } else if (message.startsWith(PacketTags.COVER_PRIV)) {
            String coverUrl = message.substring(PacketTags.COVER_PRIV.length());
            if (!coverUrl.isBlank()) {
                MusicState.get().setPersonalCover(coverUrl);
                prefetchCover(coverUrl);
            }
        } else if (message.startsWith(PacketTags.COVER)) {
            String coverUrl = message.substring(PacketTags.COVER.length());
            if (!coverUrl.isBlank()) {
                MusicState.get().setCurrentCover(coverUrl);
                prefetchCover(coverUrl);
            }
        } else if (message.startsWith(PacketTags.LYRIC_PRIV)) {
            if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
                MusicState.get().setLyrics(Lyrics.parse(message.substring(PacketTags.LYRIC_PRIV.length())));
            }
        } else if (message.startsWith(PacketTags.LYRIC)) {
            if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER) {
                MusicState.get().setLyrics(Lyrics.parse(message.substring(PacketTags.LYRIC.length())));
            }
        } else if (message.startsWith(PacketTags.SEARCH_ERROR)) {
            MusicState.get().markSearchFailed(message.substring(PacketTags.SEARCH_ERROR.length()));
        } else if (message.startsWith(PacketTags.QUEUE)) {
            handleQueue(message.substring(PacketTags.QUEUE.length()));
        } else if (message.startsWith(PacketTags.SEARCH_RESULT)) {
            handleSearchResult(message.substring(PacketTags.SEARCH_RESULT.length()));
        } else {
            ClientEvent.onPacket(message);
        }
    }

    private static void prefetchCover(String url) {
        if (url != null && !url.isBlank()) {
            CoverTextureCache.get().get(url);
        }
    }

    private static void handleQueue(String json) {
        MusicState state = MusicState.get();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            int total = getInt(root, "total", 0);
            String currentName = "";
            String currentArtist = "";
            String currentCover = "";
            if (root.has("current") && root.get("current").isJsonObject()) {
                JsonObject current = root.getAsJsonObject("current");
                currentName = getString(current, "name", "");
                currentArtist = getString(current, "artist", "");
                currentCover = getString(current, "cover", "");
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
                            "", 0,
                            getString(obj, "cover", "")));
                }
            }
            // 若队列未携带封面但当前曲目未变，保留已有的播放时封面
            if (currentCover.isBlank() && currentName.equals(state.getCurrentName())) {
                currentCover = state.getCurrentCover();
            }
            state.setQueue(total, currentName, currentArtist, currentCover, next);
            prefetchCover(currentCover);
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
                            getLong(obj, "duration", 0),
                            getString(obj, "cover", "")));
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
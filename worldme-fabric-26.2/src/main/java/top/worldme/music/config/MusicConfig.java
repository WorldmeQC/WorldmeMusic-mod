package top.worldme.music.config;

import net.fabricmc.loader.api.FabricLoader;
import top.worldme.music.WorldmeMusic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * WorldmeMusic 客户端配置（音量、模式）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public class MusicConfig {

    private static final String FILE_NAME = "worldmemusic.properties";
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

    private float volume = 1.0f;
    private int playMode = WorldmeMusic.MODE_SERVER;
    private boolean showHudLyrics = true;
    private float hudLyricsX = 0.5f;
    private float hudLyricsY = 0.85f;

    public void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            props.load(in);
            volume = clamp(parseFloat(props.getProperty("volume"), 1.0f));
            playMode = parseInt(props.getProperty("mode"), WorldmeMusic.MODE_SERVER);
            showHudLyrics = Boolean.parseBoolean(props.getProperty("hud-lyrics", "true"));
            hudLyricsX = clamp(parseFloat(props.getProperty("hud-lyrics-x"), 0.5f));
            hudLyricsY = clamp(parseFloat(props.getProperty("hud-lyrics-y"), 0.85f));
        } catch (IOException ignored) {
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("volume", String.valueOf(volume));
        props.setProperty("mode", String.valueOf(playMode));
        props.setProperty("hud-lyrics", String.valueOf(showHudLyrics));
        props.setProperty("hud-lyrics-x", String.valueOf(hudLyricsX));
        props.setProperty("hud-lyrics-y", String.valueOf(hudLyricsY));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "WorldmeMusic client config");
            }
        } catch (IOException ignored) {
        }
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = clamp(volume);
    }

    public int getPlayMode() {
        return playMode;
    }

    public void setPlayMode(int playMode) {
        this.playMode = playMode;
    }

    public boolean isShowHudLyrics() {
        return showHudLyrics;
    }

    public void setShowHudLyrics(boolean showHudLyrics) {
        this.showHudLyrics = showHudLyrics;
    }

    public float getHudLyricsX() {
        return hudLyricsX;
    }

    public void setHudLyricsX(float hudLyricsX) {
        this.hudLyricsX = clamp(hudLyricsX);
    }

    public float getHudLyricsY() {
        return hudLyricsY;
    }

    public void setHudLyricsY(float hudLyricsY) {
        this.hudLyricsY = clamp(hudLyricsY);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float parseFloat(String value, float def) {
        if (value == null) {
            return def;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(String value, int def) {
        if (value == null) {
            return def;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
package top.worldme.music.state;

/**
 * 一行歌词（时间戳 + 文本）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class LyricLine {

    private final long timeMs;
    private final String text;

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text == null ? "" : text;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getText() {
        return text;
    }
}

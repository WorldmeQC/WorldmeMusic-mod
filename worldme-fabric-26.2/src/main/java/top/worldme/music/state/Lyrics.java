package top.worldme.music.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 歌词解析器（客户端）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class Lyrics {

    private static final Pattern TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(?<seconds>\\d{2}\\.?\\d*)\\]");

    public static final Lyrics EMPTY = new Lyrics(Collections.emptyList());

    private final List<LyricLine> lines;

    public Lyrics(List<LyricLine> lines) {
        List<LyricLine> sorted = new ArrayList<>(lines);
        sorted.sort(Comparator.comparingLong(LyricLine::getTimeMs));
        this.lines = Collections.unmodifiableList(sorted);
    }

    public static Lyrics parse(String lrcText) {
        List<LyricLine> result = new ArrayList<>();
        if (lrcText == null || lrcText.isBlank()) {
            return EMPTY;
        }
        for (String rawLine : lrcText.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            List<Long> timestamps = new ArrayList<>();
            Matcher matcher = TIME_PATTERN.matcher(line);
            while (matcher.find()) {
                timestamps.add(parseTimestamp(matcher.group(1), matcher.group("seconds")));
            }
            if (timestamps.isEmpty()) {
                continue;
            }
            String text = line.replaceAll("\\[\\d{2}:\\d{2}\\.?\\d*\\]", "").trim();
            for (long timestamp : timestamps) {
                result.add(new LyricLine(timestamp, text));
            }
        }
        return new Lyrics(result);
    }

    private static long parseTimestamp(String minutes, String seconds) {
        try {
            long min = Long.parseLong(minutes);
            double sec = Double.parseDouble(seconds);
            return min * 60_000L + (long) (sec * 1000);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public List<LyricLine> getLines() {
        return lines;
    }

    /**
     * 返回指定播放时间对应的歌词行下标，找不到返回 -1。
     */
    public int indexAt(long positionMs) {
        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getTimeMs() <= positionMs) {
                index = i;
            } else {
                break;
            }
        }
        return index;
    }

    public String lineAt(int index) {
        if (index < 0 || index >= lines.size()) {
            return "";
        }
        return lines.get(index).getText();
    }
}

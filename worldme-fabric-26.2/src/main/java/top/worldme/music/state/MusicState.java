package top.worldme.music.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端音乐状态：搜索、全服歌单、本地播放标记。
 *
 * @author Worldme
 * @since 1.0.0
 */
public class MusicState {

    private static final MusicState INSTANCE = new MusicState();

    public static MusicState get() {
        return INSTANCE;
    }

    private final List<Song> searchResults = new ArrayList<>();
    private String searchKeyword = "";
    private int searchOffset = 0;
    private boolean hasSearch = false;

    private String currentName = "";
    private String currentArtist = "";
    private String currentCover = "";
    private final List<Song> next = new ArrayList<>();
    private int queueTotal = 0;
    private boolean hasQueue = false;

    private String lastServerUrl = "";
    private boolean localStopped = false;

    private final List<Song> personalQueue = new ArrayList<>();
    private int currentPersonalIndex = -1;
    private String personalCover = "";

    private Lyrics lyrics = Lyrics.EMPTY;

    private int searchVersion = 0;
    private String searchError = "";

    private int version = 0;

    private MusicState() {
    }

    /** 一首歌曲的展示数据。 */
    public static final class Song {
        public final long id;
        public final String name;
        public final String artist;
        public final String album;
        public final long duration;
        public final String coverUrl;

        public Song(long id, String name, String artist, String album, long duration, String coverUrl) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.artist = artist == null ? "" : artist;
            this.album = album == null ? "" : album;
            this.duration = duration;
            this.coverUrl = coverUrl == null ? "" : coverUrl;
        }
    }

    public synchronized void setSearchResults(String keyword, int offset, List<Song> songs) {
        searchKeyword = keyword == null ? "" : keyword;
        searchOffset = offset;
        searchResults.clear();
        if (songs != null) {
            searchResults.addAll(songs);
        }
        hasSearch = true;
        searchError = "";
        searchVersion++;
        version++;
    }

    public synchronized int getSearchVersion() {
        return searchVersion;
    }

    public synchronized String getSearchError() {
        return searchError;
    }

    public synchronized void markSearchFailed(String message) {
        searchError = message == null ? "" : message;
        searchVersion++;
    }

    public synchronized void clearSearchError() {
        searchError = "";
    }

    public synchronized List<Song> getSearchResults() {
        return Collections.unmodifiableList(searchResults);
    }

    public synchronized String getSearchKeyword() {
        return searchKeyword;
    }

    public synchronized int getSearchOffset() {
        return searchOffset;
    }

    public synchronized boolean hasSearch() {
        return hasSearch;
    }

    public synchronized void setQueue(int total, String currentName, String currentArtist,
                                      String currentCover, List<Song> nextSongs) {
        queueTotal = total;
        this.currentName = currentName == null ? "" : currentName;
        this.currentArtist = currentArtist == null ? "" : currentArtist;
        this.currentCover = currentCover == null ? "" : currentCover;
        next.clear();
        if (nextSongs != null) {
            next.addAll(nextSongs);
        }
        hasQueue = true;
        version++;
    }

    public synchronized String getCurrentName() {
        return currentName;
    }

    public synchronized String getCurrentArtist() {
        return currentArtist;
    }

    public synchronized String getCurrentCover() {
        return currentCover;
    }

    public synchronized void setCurrentCover(String coverUrl) {
        this.currentCover = coverUrl == null ? "" : coverUrl;
        version++;
    }

    public synchronized Lyrics getLyrics() {
        return lyrics;
    }

    public synchronized void setLyrics(Lyrics lyrics) {
        this.lyrics = lyrics == null ? Lyrics.EMPTY : lyrics;
        version++;
    }

    public synchronized void clearLyrics() {
        if (this.lyrics != Lyrics.EMPTY) {
            this.lyrics = Lyrics.EMPTY;
            version++;
        }
    }

    public synchronized String getPersonalCover() {
        return personalCover;
    }

    public synchronized void setPersonalCover(String coverUrl) {
        this.personalCover = coverUrl == null ? "" : coverUrl;
        version++;
    }

    /**
     * 清除个人模式下「正在播放」的标记（保留个人歌单）。
     */
    public synchronized void resetPersonalPlayback() {
        if (currentPersonalIndex != -1) {
            currentPersonalIndex = -1;
            version++;
        }
    }

    public synchronized List<Song> getNext() {
        return Collections.unmodifiableList(next);
    }

    public synchronized int getQueueTotal() {
        return queueTotal;
    }

    public synchronized boolean hasQueue() {
        return hasQueue;
    }

    public synchronized String getLastServerUrl() {
        return lastServerUrl;
    }

    public synchronized void setLastServerUrl(String url) {
        lastServerUrl = url == null ? "" : url;
    }

    public synchronized boolean isLocalStopped() {
        return localStopped;
    }

    public synchronized void setLocalStopped(boolean localStopped) {
        if (this.localStopped != localStopped) {
            this.localStopped = localStopped;
            version++;
        }
    }

    /**
     * 个人点歌：追加到本地个人歌单。
     *
     * @return 若此前没有正在播放的曲目，返回 true 表示应立即开始播放
     */
    public synchronized boolean enqueuePersonal(Song song) {
        if (song == null) {
            return false;
        }
        personalQueue.add(song);
        version++;
        if (currentPersonalIndex == -1) {
            currentPersonalIndex = personalQueue.size() - 1;
            version++;
            return true;
        }
        return false;
    }

    /**
     * 个人歌单自动续播：返回下一首；已到末尾则清除播放标记并返回 null。
     */
    public synchronized Song advancePersonal() {
        if (currentPersonalIndex < 0 || personalQueue.isEmpty()) {
            return null;
        }
        int next = currentPersonalIndex + 1;
        if (next >= personalQueue.size()) {
            currentPersonalIndex = -1;
            version++;
            return null;
        }
        currentPersonalIndex = next;
        version++;
        return personalQueue.get(next);
    }

    public synchronized List<Song> getPersonalQueue() {
        return Collections.unmodifiableList(personalQueue);
    }

    public synchronized int getCurrentPersonalIndex() {
        return currentPersonalIndex;
    }

    public synchronized int getVersion() {
        return version;
    }
}
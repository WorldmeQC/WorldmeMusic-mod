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
    private final List<Song> next = new ArrayList<>();
    private int queueTotal = 0;
    private boolean hasQueue = false;

    private String lastServerUrl = "";
    private boolean localStopped = false;

    private final List<Song> personalQueue = new ArrayList<>();
    private int currentPersonalIndex = -1;

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

        public Song(long id, String name, String artist, String album, long duration) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.artist = artist == null ? "" : artist;
            this.album = album == null ? "" : album;
            this.duration = duration;
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
        version++;
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

    public synchronized void setQueue(int total, String currentName, String currentArtist, List<Song> nextSongs) {
        queueTotal = total;
        this.currentName = currentName == null ? "" : currentName;
        this.currentArtist = currentArtist == null ? "" : currentArtist;
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

    /** 个人点歌：追加到本地个人歌单。 */
    public synchronized void addPersonal(Song song) {
        if (song == null) {
            return;
        }
        personalQueue.add(song);
        version++;
    }

    /** 标记最近一次个人点歌为当前播放。 */
    public synchronized void markPersonalPlayingLatest() {
        if (!personalQueue.isEmpty()) {
            currentPersonalIndex = personalQueue.size() - 1;
            version++;
        }
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
package top.worldme.music;

/**
 * 插件消息报文标签（1 字节 0x9A 前缀之后的文本标签）。
 *
 * <p>与 WorldmeMusic-plugin 的 {@code WorldmeMusicMessenger}/{@code ModMessageListener}
 * 保持一致，改动需两端同步。</p>
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class PacketTags {

    // 下行：服务端 -> 客户端
    public static final String PLAY = "[Play]";
    public static final String PLAY_PRIV = "[PlayPriv]";
    public static final String PLAY_PRIV_FAIL = "[PlayPrivFail]";
    public static final String STOP = "[Stop]";
    public static final String COVER = "[Cover]";
    public static final String COVER_PRIV = "[CoverPriv]";
    public static final String LYRIC = "[Lyric]";
    public static final String LYRIC_PRIV = "[LyricPriv]";
    public static final String QUEUE = "[Queue]";
    public static final String SEARCH_RESULT = "[SearchResult]";
    public static final String SEARCH_ERROR = "[SearchError]";

    // 上行：客户端 -> 服务端
    public static final String SEARCH = "[Search]";
    public static final String SEARCH_PAGE = "[SearchPage]";
    public static final String ADD_PUB = "[AddPub]";
    public static final String ADD_PRIV = "[AddPriv]";
    public static final String PLAY_PRIV_REQ = "[PlayPrivReq]";
    public static final String QUEUE_REQ = "[QueueReq]";

    private PacketTags() {
    }
}

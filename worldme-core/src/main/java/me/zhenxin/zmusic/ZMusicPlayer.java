package me.zhenxin.zmusic;

/**
 * 原生播放器 JNI 桥接基类。
 *
 * <p><b>包名与类名不可更改。</b>预编译的原生库（zmusic-player）以固定的 JNI 符号名
 * 导出函数，例如 {@code Java_me_zhenxin_zmusic_ZMusicPlayer_nativeInit}。
 * JNI 符号名由「声明 native 方法的类的全限定名」决定，因此必须让声明 native
 * 方法的类保持为 {@code me.zhenxin.zmusic.ZMusicPlayer}，{@link
 * top.worldme.music.WorldmeMusicPlayer} 继承本类后即可正常调用这些原生方法。</p>
 *
 * <p>原生方法签名需与 zmusic-player 的原生实现严格一致。</p>
 *
 * @author 真心
 * @since 2026-04-24 00:00
 */
public abstract class ZMusicPlayer {

    // ---- Native 方法声明 ----

    // --- 生命周期管理 ---
    protected native long nativeInit();

    protected native void nativeDestroy(long handle);

    // --- 播放控制 ---
    protected native int nativePlay(long handle, String url);

    protected native int nativePause(long handle);

    protected native int nativeStop(long handle);

    protected native int nativeResume(long handle);

    protected native int nativeSeek(long handle, long positionMs);

    // --- 状态查询 ---
    protected native int nativeGetState(long handle);

    protected native long nativeGetPosition(long handle);

    protected native long nativeGetDuration(long handle);

    protected native float nativeGetVolume(long handle);

    protected native int nativeSetVolume(long handle, float volume);

    // --- 队列操作 ---
    protected native void nativeEnqueue(long handle, String url, String title, String artist);

    protected native void nativeEnqueueNext(long handle, String url, String title, String artist);

    protected native void nativeRemoveFromQueue(long handle, int index);

    protected native void nativeClearQueue(long handle);

    protected native void nativePlayNext(long handle);

    protected native void nativePlayPrevious(long handle);

    protected native void nativePlayAtIndex(long handle, int index);

    protected native int nativeGetQueueSize(long handle);

    protected native int nativeGetCurrentIndex(long handle);

    // --- 歌词 ---
    protected native void nativeLoadLyrics(long handle, String lrcContent);

    protected native String nativeGetCurrentLyric(long handle);

    protected native String nativeGetLyricLineAt(long handle, long timeMs);

    // --- 模式控制 ---
    protected native void nativeSetRepeatMode(long handle, int mode);

    protected native void nativeSetShuffle(long handle, boolean enabled);

    // --- 事件轮询 ---
    protected native int nativePollEvent(long handle);
}
# AGENTS.md

## 项目简介

WorldmeMusic-mod 是一个**纯客户端** Minecraft 音乐 mod，复刻自 [zmusic-mod](https://github.com/starhui-dev/zmusic-mod) 的 Fabric 26.2 版本，仅保留该一个加载器 × 版本（GPL-3.0）。代码拆成 `worldme-core` 核心模块与 `worldme-fabric-26.2` 叶子子项目：

- `worldme-core`（Java 8）：纯逻辑，不依赖任何 MC API，通过 JNI 桥接外部 Rust 播放引擎（zmusic-player），含主入口 `WorldmeMusic`、桥接 `WorldmeMusicPlayer`、平台无关事件 `ClientEvent`/`PacketEvent`、`SoundManager` 接口。
- `worldme-fabric-26.2`：依赖 MC 26.2 / Fabric，提供 mod 入口、`SoundManager` 实现、拦截原版音乐的 mixin。

服务端插件（如 WorldmeMusic-plugin）经插件消息通道 `zmusic:channel` 下发「1 字节前缀 + UTF-8 文本」（`[Play]url` / `[Stop]`），客户端跳过首字节解析后驱动原生播放器播放。

## 编译流程

构建需要 **JDK 25**（`worldme-fabric-26.2` 声明 Java 25 toolchain；`worldme-core` 以 Java 8 目标编译，JDK 25 仍支持 `-source 8`）。构建联网：需下载 MC 26.2 / Fabric 依赖，以及从 `starhui-dev/zmusic-player` GitHub Release 拉取 7 个平台的原生库（受 GitHub API 限流影响）。wrapper 为 Gradle 9.5.1。

```bash
mise run build
# 或直接用仓库自带 wrapper
./gradlew clean build --no-daemon
```

构建单个版本：

```bash
./gradlew :worldme-fabric-26.2:build --no-daemon
```

**本仓库没有任何测试代码**，验证手段是「能否编译出 jar」。

## 打包后目录

产物：`worldme-fabric-26.2/build/libs/worldme-fabric-26.2-2.0.0.jar`（约 11.4 MB），jar 内容：

- `top/worldme/music/**`：core + fabric 全部 class（含 mixin）
- `META-INF/native/`：7 平台原生库 `libzmusic.so` / `zmusic.dll` / `libzmusic.dylib`（x86_64/aarch64 × linux/windows/macos + aarch64-android）
- `fabric.mod.json`、`worldme.mixins.json`、`icon.png`、`LICENSE`

## 架构

### 跨平台关注点

1. **抑制原版音乐**：播放器处于 `STATE_PLAYING` 时，mixin 注入 `SoundEngine.play*`（`mixin/SoundEvent.java`）取消 `MUSIC`/`RECORDS` 分类的声音；每 tick 把游戏的 RECORDS 音量同步给原生播放器（`mixin/Tick.java`）。
2. **网络协议**：服务端经 `zmusic:channel` 下发（与 zmusic-plugin 兼容，**通道名不可改**），格式「1 字节前缀 + UTF-8 文本」，客户端跳过首字节解析交给 `ClientEvent.onPacket`，按 `[Play]xxx` / `[Stop]` 分发。
3. **原生库加载**（`WorldmeMusicPlayer.loadNativeLibrary`）：先试 `System.loadLibrary("zmusic")`，失败则从 jar 内 `META-INF/native/<platform>/` 提取，按 SHA-256 哈希命名缓存到游戏目录下的 `zmusic/`，校验后 `System.load`。游戏目录通过反射依次探测 Fabric→Forge→NeoForge→旧版 `Minecraft.mcDataDir` 获得，核心模块不直接依赖任何加载器。

### 原生库打包

`downloadNativeLibs` 任务从 `starhui-dev/zmusic-player` 的 GitHub Release（tag `v${zmusicPlayerVersion}`，版本在 `gradle.properties`）拉取原生库，解压缓存到 `.gradle/zmusic-player/`，叶子项目打 jar 时塞进 `META-INF/native/`。

## 约定与陷阱

- **命名**：mod id `worldmemusic`，Java 包 `top.worldme.music`，group `top.worldme`，版本 `2.0.0`。原生库文件名（`zmusic.dll`/`libzmusic.so`/`libzmusic.dylib`）与 `zmusic:channel` 通道来自 zmusic 生态，保持不变。
- **JNI 桥接类不可改名**：native 方法声明在 `me.zhenxin.zmusic.ZMusicPlayer`（`worldme-core`），`WorldmeMusicPlayer` 继承它。预编译的 zmusic-player 原生库以固定 JNI 符号名导出（`Java_me_zhenxin_zmusic_ZMusicPlayer_nativeInit` 等），该包名/类名一旦改动会导致 `UnsatisfiedLinkError`。
- `worldme-core` 用 Lombok（`@Log4j2`/`@Getter` 等），log4j-api 是 `compileOnly`（运行时由 MC 提供）。
- 代码注释、javadoc 以中文为主，沿用现有风格。
- 授权：GPL-3.0，LICENSE 保留原项目（zmusic-mod）版权归属。
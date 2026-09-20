package top.worldme.music.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 封面贴图缓存：异步下载网络封面，在渲染线程解码并注册为动态贴图。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class CoverTextureCache {

    private static final Logger LOGGER = LogManager.getLogger("WorldmeMusic");
    private static final CoverTextureCache INSTANCE = new CoverTextureCache();
    private static final int MAX_ENTRIES = 32;
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, Identifier> ready = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<String, Boolean> pending = new ConcurrentHashMap<>();
    private final Map<String, Integer> failed = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger();

    private CoverTextureCache() {
    }

    public static CoverTextureCache get() {
        return INSTANCE;
    }

    /**
     * 获取封面贴图；未就绪时返回 null 并触发一次异步下载。
     */
    public Identifier get(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Integer attempts = failed.get(url);
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            return null;
        }
        synchronized (ready) {
            Identifier id = ready.get(url);
            if (id != null) {
                return id;
            }
        }
        if (pending.putIfAbsent(url, Boolean.TRUE) == null) {
            download(url);
        }
        return null;
    }

    private void download(String url) {
        Thread thread = new Thread(() -> {
            byte[] bytes = null;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(withSize(url)))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "WorldmeMusic/2.0.0")
                        .header("Referer", "https://music.163.com/")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    bytes = response.body();
                } else {
                    LOGGER.warn("封面下载失败 HTTP {}: {}", response.statusCode(), url);
                }
            } catch (Exception e) {
                LOGGER.warn("封面下载异常: {} ({})", url, e.toString());
            }
            final byte[] data = bytes;
            Minecraft.getInstance().execute(() -> register(url, data));
        }, "worldme-cover");
        thread.setDaemon(true);
        thread.start();
    }

    private void register(String url, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            markFailed(url);
            pending.remove(url);
            return;
        }
        NativeImage image = null;
        try {
            image = decode(bytes);
            Identifier id = Identifier.fromNamespaceAndPath("worldmemusic",
                    "cover/" + Integer.toHexString(idSequence.incrementAndGet()));
            DynamicTexture texture = new DynamicTexture(() -> "worldmemusic cover", image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            synchronized (ready) {
                ready.put(url, id);
                evictIfNeeded();
            }
            LOGGER.info("封面加载成功: {}", url);
        } catch (Exception e) {
            if (image != null) {
                image.close();
            }
            markFailed(url);
            LOGGER.warn("封面解码/注册失败: {} ({})", url, e.toString());
        } finally {
            pending.remove(url);
        }
    }

    private void markFailed(String url) {
        failed.merge(url, 1, Integer::sum);
    }

    /**
     * 网易云封面多为 JPEG，而 NativeImage.read 仅支持 PNG；这里用 ImageIO 解码后写入 NativeImage。
     * setPixel 接收 ARGB，与 BufferedImage.getRGB 一致。
     */
    private static NativeImage decode(byte[] bytes) throws IOException {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        if (decoded == null) {
            throw new IOException("不支持的图片格式");
        }
        int width = decoded.getWidth();
        int height = decoded.getHeight();
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, decoded.getRGB(x, y));
            }
        }
        return image;
    }

    private void evictIfNeeded() {
        while (ready.size() > MAX_ENTRIES) {
            Iterator<Map.Entry<String, Identifier>> iterator = ready.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Map.Entry<String, Identifier> eldest = iterator.next();
            iterator.remove();
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
        }
    }

    private String withSize(String url) {
        if (url.contains("param=")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "param=128y128";
    }
}

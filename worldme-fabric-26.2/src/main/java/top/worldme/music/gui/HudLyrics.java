package top.worldme.music.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import top.worldme.music.WorldmeMusicClient;
import top.worldme.music.config.MusicConfig;
import top.worldme.music.manager.PlaybackProgress;
import top.worldme.music.state.Lyrics;
import top.worldme.music.state.MusicState;

/**
 * 游戏内 HUD 歌词：显示当前行（高亮）与下一句，位置可在设置中调整。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class HudLyrics {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 4;

    private HudLyrics() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        MusicConfig config = WorldmeMusicClient.CONFIG;
        if (config == null || !config.isShowHudLyrics()) {
            return;
        }
        MusicState state = MusicState.get();
        Lyrics lyrics = state.getLyrics();
        if (lyrics.isEmpty()) {
            return;
        }
        int current = Math.max(0, lyrics.indexAt(PlaybackProgress.getPositionMs()));
        String currentText = lyrics.lineAt(current);
        String nextText = lyrics.lineAt(current + 1);
        if (currentText.isEmpty() && nextText.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int boxWidth = Math.max(font.width(currentText), font.width(nextText)) + PADDING * 2;
        int centerX = (int) (screenWidth * config.getHudLyricsX());
        int baseY = (int) (screenHeight * config.getHudLyricsY());
        centerX = Math.max(boxWidth / 2, Math.min(screenWidth - boxWidth / 2, centerX));
        baseY = Math.max(PADDING, Math.min(screenHeight - LINE_HEIGHT * 2 - PADDING, baseY));

        int boxLeft = centerX - boxWidth / 2;
        int boxTop = baseY - PADDING;
        int boxBottom = baseY + LINE_HEIGHT + (nextText.isEmpty() ? 0 : LINE_HEIGHT) + PADDING;
        graphics.fill(boxLeft, boxTop, boxLeft + boxWidth, boxBottom, 0x66000000);

        graphics.text(font, currentText, centerX - font.width(currentText) / 2, baseY, 0xFFFFFF55);
        if (!nextText.isEmpty()) {
            graphics.text(font, nextText, centerX - font.width(nextText) / 2, baseY + LINE_HEIGHT, 0xFFAAAAAA);
        }
    }
}

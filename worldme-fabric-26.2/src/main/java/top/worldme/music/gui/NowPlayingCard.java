package top.worldme.music.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import top.worldme.music.manager.PlaybackProgress;
import top.worldme.music.state.LyricLine;
import top.worldme.music.state.Lyrics;
import top.worldme.music.state.MusicState;

import java.util.List;

/**
 * 「正在播放」卡片渲染：封面 + 歌曲信息 + 进度 + 滚动歌词。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class NowPlayingCard {

    public static final int TOP = 54;
    public static final int COVER_SIZE = 96;
    private static final int LINE_HEIGHT = 12;

    private NowPlayingCard() {
    }

    public static int width(int screenWidth) {
        return Math.min(440, screenWidth - 40);
    }

    public static int cardX(int screenWidth) {
        return (screenWidth - width(screenWidth)) / 2;
    }

    public static int buttonX(int screenWidth) {
        return cardX(screenWidth) + COVER_SIZE + 14;
    }

    public static int buttonY() {
        return TOP + 74;
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight,
                              MusicState state, CoverTextureCache coverCache) {
        int cardWidth = width(screenWidth);
        int cardX = cardX(screenWidth);
        int coverY = TOP;
        int rightX = cardX + COVER_SIZE + 14;
        int rightW = cardX + cardWidth - rightX;

        graphics.fill(cardX - 6, coverY - 6, cardX + cardWidth + 6, coverY + COVER_SIZE + 6, 0x66000000);

        PlayerControl.NowPlaying now = PlayerControl.resolve(state);
        Identifier cover = coverCache.get(now.coverUrl);
        if (cover != null) {
            graphics.blit(cover, cardX, coverY, cardX + COVER_SIZE, coverY + COVER_SIZE, 0f, 1f, 0f, 1f);
        } else {
            graphics.fill(cardX, coverY, cardX + COVER_SIZE, coverY + COVER_SIZE, 0xFF1E1E1E);
            String note = "♪";
            graphics.text(font, note, cardX + (COVER_SIZE - font.width(note)) / 2,
                    coverY + COVER_SIZE / 2 - 4, 0xFF808080);
        }

        if (now.isEmpty()) {
            graphics.text(font, "当前没有播放中的歌曲", rightX, coverY + 4, 0xFFAAAAAA);
        } else {
            graphics.text(font, now.name, rightX, coverY + 2, 0xFFFFFFFF);
            graphics.text(font, now.artist, rightX, coverY + 16, 0xFFAAAAAA);

            int barY = coverY + 52;
            int barRight = rightX + rightW;
            graphics.fill(rightX, barY, barRight, barY + 6, 0xFF3A3A3A);
            graphics.fill(rightX, barY, rightX + (int) (rightW * PlaybackProgress.getProgress()), barY + 6, 0xFF55FF55);
            graphics.text(font,
                    PlayerControl.formatTime(PlaybackProgress.getPositionMs()) + " / "
                            + PlayerControl.formatTime(PlaybackProgress.getDurationMs()),
                    rightX, barY + 9, 0xFFCCCCCC);
        }

        renderLyrics(graphics, font, cardX, cardWidth, coverY + COVER_SIZE + 14, screenHeight, state);
    }

    private static void renderLyrics(GuiGraphicsExtractor graphics, Font font, int cardX, int cardWidth,
                                     int lyricTop, int screenHeight, MusicState state) {
        int lyricBottom = screenHeight - 16;
        if (lyricBottom <= lyricTop + LINE_HEIGHT) {
            return;
        }
        graphics.fill(cardX - 6, lyricTop - 6, cardX + cardWidth + 6, lyricBottom + 6, 0x66000000);
        graphics.enableScissor(cardX, lyricTop, cardX + cardWidth, lyricBottom);
        Lyrics lyrics = state.getLyrics();
        if (lyrics.isEmpty()) {
            String hint = "暂无歌词";
            graphics.text(font, hint, cardX + (cardWidth - font.width(hint)) / 2,
                    (lyricTop + lyricBottom) / 2 - 4, 0xFFAAAAAA);
        } else {
            int current = lyrics.indexAt(PlaybackProgress.getPositionMs());
            int center = Math.max(0, current);
            int centerY = (lyricTop + lyricBottom) / 2;
            List<LyricLine> lines = lyrics.getLines();
            for (int i = 0; i < lines.size(); i++) {
                int y = centerY + (i - center) * LINE_HEIGHT - 4;
                if (y < lyricTop - LINE_HEIGHT || y > lyricBottom) {
                    continue;
                }
                String text = lines.get(i).getText();
                if (text.isEmpty()) {
                    continue;
                }
                int distance = Math.abs(i - current);
                int color = i == current ? 0xFFFFFFFF : (distance == 1 ? 0xFFBBBBBB : 0xFF777777);
                graphics.text(font, text, cardX + (cardWidth - font.width(text)) / 2, y, color);
            }
        }
        graphics.disableScissor();
    }
}

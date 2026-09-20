package top.worldme.music.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import top.worldme.music.config.MusicConfig;

/**
 * HUD 歌词位置滑块（水平 / 垂直）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class PositionSlider extends AbstractSliderButton {

    private final MusicConfig config;
    private final boolean horizontal;

    public PositionSlider(int x, int y, int width, int height, MusicConfig config, boolean horizontal) {
        super(x, y, width, height, Component.empty(),
                horizontal ? config.getHudLyricsX() : config.getHudLyricsY());
        this.config = config;
        this.horizontal = horizontal;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal((horizontal ? "歌词水平位置：" : "歌词垂直位置：")
                + Math.round(value * 100) + "%"));
    }

    @Override
    protected void applyValue() {
        if (horizontal) {
            config.setHudLyricsX((float) value);
        } else {
            config.setHudLyricsY((float) value);
        }
        config.save();
    }
}

package top.worldme.music.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.WorldmeMusicPlayer;
import top.worldme.music.config.MusicConfig;

/**
 * 音量滑块。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class VolumeSlider extends AbstractSliderButton {

    private final MusicConfig config;

    public VolumeSlider(int x, int y, int width, int height, MusicConfig config) {
        super(x, y, width, height, Component.empty(), config.getVolume());
        this.config = config;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal("音量：" + Math.round(value * 100) + "%"));
    }

    @Override
    protected void applyValue() {
        config.setVolume((float) value);
        config.save();
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player != null) {
            player.setVolume((float) value);
        }
    }
}

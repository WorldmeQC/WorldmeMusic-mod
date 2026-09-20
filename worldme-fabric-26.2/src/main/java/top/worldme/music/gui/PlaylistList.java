package top.worldme.music.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.state.MusicState;

import java.util.List;

/**
 * 播放列表（个人歌单 / 全服队列）。点击正在播放的条目可切到播放界面。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class PlaylistList extends ObjectSelectionList<PlaylistList.Entry> {

    public static final int ROW_HEIGHT = 22;

    private final Minecraft minecraft;
    private final Runnable openPlayer;

    public PlaylistList(Minecraft minecraft, int width, int height, int y0, int itemHeight, Runnable openPlayer) {
        super(minecraft, width, height, y0, itemHeight);
        this.minecraft = minecraft;
        this.openPlayer = openPlayer;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getRight(), getBottom(), 0x66000000);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Entry entry = getEntryAtPosition(event.x(), event.y());
        if (entry != null && entry.playing) {
            openPlayer.run();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public void refresh() {
        clearEntries();
        MusicState state = MusicState.get();
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            refreshPersonal(state);
        } else {
            refreshServer(state);
        }
    }

    private void refreshPersonal(MusicState state) {
        List<MusicState.Song> queue = state.getPersonalQueue();
        if (queue.isEmpty()) {
            addEntry(new Entry(minecraft, "个人播放列表为空，去「点歌」页搜索点歌吧", false, 0xFFAAAAAA));
            return;
        }
        int current = state.getCurrentPersonalIndex();
        int index = 1;
        for (int i = 0; i < queue.size(); i++) {
            MusicState.Song song = queue.get(i);
            boolean playing = (i == current);
            String text = (playing ? "▶ " : "") + index + ". " + song.name + " - " + song.artist;
            addEntry(new Entry(minecraft, text, playing, playing ? 0xFFFFFF55 : 0xFFFFFFFF));
            index++;
        }
    }

    private void refreshServer(MusicState state) {
        if (!state.hasQueue()) {
            addEntry(new Entry(minecraft, "正在请求服务器歌单...", false, 0xFFFFFFFF));
            return;
        }
        if (state.getCurrentName().isEmpty() && state.getNext().isEmpty()) {
            addEntry(new Entry(minecraft, "当前没有播放中的歌曲", false, 0xFFAAAAAA));
            return;
        }
        int index = 0;
        if (!state.getCurrentName().isEmpty()) {
            addEntry(new Entry(minecraft,
                    "▶ " + state.getCurrentName() + " - " + state.getCurrentArtist(), true, 0xFFFFFF55));
            index = 1;
        }
        for (MusicState.Song song : state.getNext()) {
            addEntry(new Entry(minecraft,
                    index + ". " + song.name + " - " + song.artist, false, 0xFFFFFFFF));
            index++;
        }
    }

    /**
     * 播放列表条目。
     */
    public static final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final Minecraft minecraft;
        private final String text;
        private final boolean playing;
        private final int color;

        Entry(Minecraft minecraft, String text, boolean playing, int color) {
            this.minecraft = minecraft;
            this.text = text;
            this.playing = playing;
            this.color = color;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = getX();
            int y = getY();
            int right = getX() + getWidth();
            if (hovered) {
                graphics.fill(x, y, right, getY() + getHeight(), 0x20FFFFFF);
            }
            String line = text;
            int maxText = right - x - 16;
            if (maxText > 8) {
                line = minecraft.font.plainSubstrByWidth(line, maxText);
            }
            int textY = y + (ROW_HEIGHT - minecraft.font.lineHeight) / 2;
            graphics.text(minecraft.font, line, x + 6, textY, color);
        }

        @Override
        public int getHeight() {
            return ROW_HEIGHT;
        }

        @Override
        public Component getNarration() {
            return Component.literal(text);
        }
    }
}

package top.worldme.music.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.net.ModNetwork;
import top.worldme.music.state.MusicState;

import java.util.List;

/**
 * 搜索结果列表（点击条目点歌）。
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class SearchResultList extends ObjectSelectionList<SearchResultList.Entry> {

    public static final int ROW_HEIGHT = 22;

    private final Minecraft minecraft;

    public SearchResultList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
        super(minecraft, width, height, y0, itemHeight);
        this.minecraft = minecraft;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getRight(), getBottom(), 0x66000000);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Entry entry = getEntryAtPosition(event.x(), event.y());
        if (entry != null) {
            entry.addSong();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public void refresh() {
        clearEntries();
        List<MusicState.Song> songs = MusicState.get().getSearchResults();
        int index = 1;
        for (MusicState.Song song : songs) {
            addEntry(new Entry(minecraft, song, index));
            index++;
        }
    }

    /**
     * 搜索结果条目。
     */
    public static final class Entry extends ObjectSelectionList.Entry<Entry> {

        private final Minecraft minecraft;
        private final MusicState.Song song;
        private final int index;

        Entry(Minecraft minecraft, MusicState.Song song, int index) {
            this.minecraft = minecraft;
            this.song = song;
            this.index = index;
        }

        void addSong() {
            if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
                if (MusicState.get().enqueuePersonal(song)) {
                    ModNetwork.sendPlayPrivateReq(song.id);
                }
            } else {
                ModNetwork.sendAddPub(song.id);
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = getX();
            int y = getY();
            int right = getX() + getWidth();
            if (hovered) {
                graphics.fill(x, y, right, getY() + getHeight(), 0x20FFFFFF);
            }
            String line = index + ". " + song.name + " - " + song.artist;
            int maxText = right - x - 64;
            if (maxText > 8) {
                line = minecraft.font.plainSubstrByWidth(line, maxText);
            }
            int textY = y + (ROW_HEIGHT - minecraft.font.lineHeight) / 2;
            graphics.text(minecraft.font, line, x + 6, textY, 0xFFFFFFFF);
            String hint = "[点歌]";
            graphics.text(minecraft.font, hint, right - minecraft.font.width(hint) - 6, textY, 0xFF55FF55);
        }

        @Override
        public int getHeight() {
            return ROW_HEIGHT;
        }

        @Override
        public Component getNarration() {
            return Component.literal(index + ". " + song.name + " - " + song.artist);
        }
    }
}

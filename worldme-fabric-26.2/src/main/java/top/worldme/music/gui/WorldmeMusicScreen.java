package top.worldme.music.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.WorldmeMusicPlayer;
import top.worldme.music.config.MusicConfig;
import top.worldme.music.net.ModNetwork;
import top.worldme.music.state.MusicState;

import java.util.List;

/**
 * WorldmeMusic 客户端 GUI（简约风格）。
 *
 * <p>三个标签页：点歌 / 播放列表 / 设置。</p>
 *
 * @author Worldme
 * @since 1.0.0
 */
public class WorldmeMusicScreen extends Screen {

    private static final int ROW_HEIGHT = 22;

    private final MusicState state = MusicState.get();
    private final MusicConfig config;

    private int currentTab = 0;
    private int lastVersion = -1;

    private Button tabRequestButton;
    private Button tabPlaylistButton;
    private Button tabSettingsButton;

    private EditBox searchBox;
    private Button searchButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private ResultList resultList;
    private StringWidget pageInfo;

    private PlaylistList playlistList;
    private StringWidget statusInfo;
    private Button stopButton;
    private Button resumeButton;

    private VolumeSlider volumeSlider;
    private Button modeButton;

    public WorldmeMusicScreen(MusicConfig config) {
        super(Component.literal("WorldmeMusic"));
        this.config = config;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        buildTabs();
        switch (currentTab) {
            case 0 -> buildRequestTab();
            case 1 -> buildPlaylistTab();
            default -> buildSettingsTab();
        }
    }

    private void buildTabs() {
        int y = 28;
        int w = 90;
        int gap = 6;
        int total = w * 3 + gap * 2;
        int x0 = (width - total) / 2;

        tabRequestButton = Button.builder(Component.literal("点歌"), b -> switchTab(0))
                .bounds(x0, y, w, 20).build();
        tabPlaylistButton = Button.builder(Component.literal("播放列表"), b -> switchTab(1))
                .bounds(x0 + w + gap, y, w, 20).build();
        tabSettingsButton = Button.builder(Component.literal("设置"), b -> switchTab(2))
                .bounds(x0 + (w + gap) * 2, y, w, 20).build();
        addRenderableWidget(tabRequestButton);
        addRenderableWidget(tabPlaylistButton);
        addRenderableWidget(tabSettingsButton);

        addRenderableWidget(new StringWidget(width / 2 - font.width("WorldmeMusic") / 2, 6,
                Component.literal("WorldmeMusic"), font));
    }

    private void switchTab(int tab) {
        if (currentTab == tab) {
            return;
        }
        currentTab = tab;
        if (tab == 1) {
            ModNetwork.sendQueueReq();
        }
        rebuild();
    }

    // ---- 点歌页 ----

    private void buildRequestTab() {
        int rowY = 54;
        int boxWidth = 180;
        int btnWidth = 60;
        int total = boxWidth + 6 + btnWidth;
        int x0 = (width - total) / 2;

        searchBox = new EditBox(font, x0, rowY, boxWidth, 20, Component.literal("搜索歌曲"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(s -> {
        });
        if (state.hasSearch()) {
            searchBox.setValue(state.getSearchKeyword());
        }
        searchButton = Button.builder(Component.literal("搜索"), b -> doSearch())
                .bounds(x0 + boxWidth + 6, rowY, btnWidth, 20).build();
        addRenderableWidget(searchBox);
        addRenderableWidget(searchButton);

        resultList = new ResultList(minecraft, width, (height - 68) - (rowY + 28), rowY + 28, ROW_HEIGHT);
        resultList.refresh();
        addRenderableWidget(resultList);

        pageInfo = new StringWidget(0, height - 46, Component.empty(), font);
        pageInfo.setX(width / 2 - pageInfo.getWidth() / 2);
        pageInfo.setMessage(buildPageInfo());
        addRenderableWidget(pageInfo);

        prevPageButton = Button.builder(Component.literal("上一页"), b -> doPage("prev"))
                .bounds(width / 2 - 130, height - 44, 110, 20).build();
        nextPageButton = Button.builder(Component.literal("下一页"), b -> doPage("next"))
                .bounds(width / 2 + 20, height - 44, 110, 20).build();
        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);
    }

    private void doSearch() {
        String keyword = searchBox.getValue();
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        ModNetwork.sendSearch(keyword.trim());
    }

    private void doPage(String action) {
        ModNetwork.sendSearchPage(action);
    }

    private Component buildPageInfo() {
        if (!state.hasSearch()) {
            return Component.literal("");
        }
        int limit = 10;
        int page = state.getSearchOffset() / Math.max(1, limit) + 1;
        return Component.literal("第 " + page + " 页");
    }

    // ---- 播放列表页 ----

    private void buildPlaylistTab() {
        statusInfo = new StringWidget(width / 2, 52, Component.empty(), font);
        statusInfo.setX(width / 2 - statusInfo.getWidth() / 2);
        addRenderableWidget(statusInfo);

        playlistList = new PlaylistList(minecraft, width, (height - 68) - 60, 60, ROW_HEIGHT);
        playlistList.refresh();
        addRenderableWidget(playlistList);

        stopButton = Button.builder(Component.literal("停止播放"), b -> doStop())
                .bounds(width / 2 - 140, height - 44, 130, 20).build();
        resumeButton = Button.builder(Component.literal("继续播放"), b -> doResume())
                .bounds(width / 2 + 10, height - 44, 130, 20).build();
        addRenderableWidget(stopButton);
        addRenderableWidget(resumeButton);
    }

    private void doStop() {
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player == null) {
            return;
        }
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER) {
            state.setLocalStopped(true);
        }
        player.stopAsync();
    }

    private void doResume() {
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player == null) {
            return;
        }
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER) {
            String url = state.getLastServerUrl();
            if (state.isLocalStopped() && !url.isEmpty()) {
                state.setLocalStopped(false);
                player.playAsync(url);
            } else {
                player.resume();
            }
        } else {
            player.resume();
        }
    }

    // ---- 设置页 ----

    private void buildSettingsTab() {
        volumeSlider = new VolumeSlider(width / 2 - 100, 90, 200, 20, config);
        addRenderableWidget(volumeSlider);

        modeButton = Button.builder(buildModeLabel(), b -> toggleMode())
                .bounds(width / 2 - 100, 130, 200, 20).build();
        addRenderableWidget(modeButton);

        StringWidget hint = new StringWidget(0, 170, Component.empty(), font);
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            hint.setMessage(Component.literal("个人模式：自己点歌自己听，不接收全服播放"));
        } else {
            hint.setMessage(Component.literal("全服模式：自动接收全服播放，可本地停止当前歌曲"));
        }
        hint.setX(width / 2 - hint.getWidth() / 2);
        addRenderableWidget(hint);
    }

    private void toggleMode() {
        int mode = WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER
                ? WorldmeMusic.MODE_PERSONAL : WorldmeMusic.MODE_SERVER;
        WorldmeMusic.setPlayMode(mode);
        config.setPlayMode(mode);
        config.save();
        modeButton.setMessage(buildModeLabel());
        // 个人模式下如果正在播放全服歌曲，停止本地播放
        if (mode == WorldmeMusic.MODE_PERSONAL && WorldmeMusic.getPlayer() != null) {
            WorldmeMusic.getPlayer().stopAsync();
        }
    }

    private Component buildModeLabel() {
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            return Component.literal("模式：个人（自己点歌自己听）");
        }
        return Component.literal("模式：全服（自动接收全服播放）");
    }

    @Override
    public void tick() {
        super.tick();
        int version = state.getVersion();
        if (version != lastVersion) {
            lastVersion = version;
            if (currentTab == 0 && resultList != null) {
                resultList.refresh();
                if (pageInfo != null) {
                    pageInfo.setMessage(buildPageInfo());
                    pageInfo.setX(width / 2 - pageInfo.getWidth() / 2);
                }
            }
            if (currentTab == 1 && playlistList != null) {
                playlistList.refresh();
                updateStatusInfo();
            }
        }
    }

    private void updateStatusInfo() {
        if (statusInfo == null) {
            return;
        }
        String text;
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            text = "个人模式：播放列表仅自己可见，点歌自己听";
        } else if (state.isLocalStopped()) {
            text = "已本地停止当前歌曲（仅影响自己）";
        } else {
            text = "全服模式：自动接收全服播放";
        }
        statusInfo.setMessage(Component.literal(text));
        statusInfo.setX(width / 2 - statusInfo.getWidth() / 2);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractMenuBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
        if (searchBox != null && searchBox.isFocused()) {
            if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
                doSearch();
                return true;
            }
            if (searchBox.keyPressed(keyEvent)) {
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 结果列表 ----

    private static final class ResultList extends ObjectSelectionList<ResultEntry> {

        private final Minecraft minecraft;

        ResultList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
            super(minecraft, width, height, y0, itemHeight);
            this.minecraft = minecraft;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            int x1 = getRight();
            int y1 = getBottom();
            graphics.fill(x0, y0, x1, y1, 0x66000000);
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            ResultEntry entry = getEntryAtPosition(event.x(), event.y());
            if (entry != null) {
                entry.addSong();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        void refresh() {
            clearEntries();
            List<MusicState.Song> songs = MusicState.get().getSearchResults();
            int index = 1;
            for (MusicState.Song song : songs) {
                addEntry(new ResultEntry(minecraft, song, index));
                index++;
            }
        }
    }

    private static final class ResultEntry extends ObjectSelectionList.Entry<ResultEntry> {

        private final Minecraft minecraft;
        private final MusicState.Song song;
        private final int index;

        ResultEntry(Minecraft minecraft, MusicState.Song song, int index) {
            this.minecraft = minecraft;
            this.song = song;
            this.index = index;
        }

        void addSong() {
            if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
                MusicState.get().addPersonal(song);
                ModNetwork.sendAddPriv(song.id);
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

    // ---- 播放列表 ----

    private static final class PlaylistList extends ObjectSelectionList<PlaylistEntry> {

        private final Minecraft minecraft;

        PlaylistList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
            super(minecraft, width, height, y0, itemHeight);
            this.minecraft = minecraft;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            int x1 = getRight();
            int y1 = getBottom();
            graphics.fill(x0, y0, x1, y1, 0x66000000);
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        }

        void refresh() {
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
                addEntry(new PlaylistEntry(minecraft, "个人播放列表为空，去「点歌」页搜索点歌吧", false, 0xFFAAAAAA));
                return;
            }
            int current = state.getCurrentPersonalIndex();
            int index = 1;
            for (int i = 0; i < queue.size(); i++) {
                MusicState.Song song = queue.get(i);
                boolean playing = (i == current);
                String text = (playing ? "▶ " : "") + index + ". " + song.name + " - " + song.artist;
                addEntry(new PlaylistEntry(minecraft, text, playing, playing ? 0xFFFFFF55 : 0xFFFFFFFF));
                index++;
            }
        }

        private void refreshServer(MusicState state) {
            if (!state.hasQueue()) {
                addEntry(new PlaylistEntry(minecraft, "正在请求服务器歌单...", false, 0xFFFFFFFF));
                return;
            }
            if (state.getCurrentName().isEmpty() && state.getNext().isEmpty()) {
                addEntry(new PlaylistEntry(minecraft, "当前没有播放中的歌曲", false, 0xFFAAAAAA));
                return;
            }
            int index = 0;
            if (!state.getCurrentName().isEmpty()) {
                addEntry(new PlaylistEntry(minecraft,
                        "▶ " + state.getCurrentName() + " - " + state.getCurrentArtist(), true, 0xFFFFFF55));
                index = 1;
            }
            for (MusicState.Song song : state.getNext()) {
                addEntry(new PlaylistEntry(minecraft,
                        index + ". " + song.name + " - " + song.artist, false, 0xFFFFFFFF));
                index++;
            }
        }
    }

    private static final class PlaylistEntry extends ObjectSelectionList.Entry<PlaylistEntry> {

        private final Minecraft minecraft;
        private final String text;
        private final boolean playing;
        private final int color;

        PlaylistEntry(Minecraft minecraft, String text, boolean playing, int color) {
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

    // ---- 音量滑块 ----

    private static final class VolumeSlider extends AbstractSliderButton {

        private final MusicConfig config;

        VolumeSlider(int x, int y, int width, int height, MusicConfig config) {
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
}
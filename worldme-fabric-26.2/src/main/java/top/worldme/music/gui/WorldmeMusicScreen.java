package top.worldme.music.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import top.worldme.music.WorldmeMusic;
import top.worldme.music.config.MusicConfig;
import top.worldme.music.manager.PersonalPlayback;
import top.worldme.music.net.ModNetwork;
import top.worldme.music.state.MusicState;

/**
 * WorldmeMusic 客户端 GUI。
 *
 * <p>四个标签页：播放界面 / 点歌 / 播放列表 / 设置。</p>
 *
 * @author Worldme
 * @since 1.0.0
 */
public class WorldmeMusicScreen extends Screen {

    private static final int PLAYLIST_TOP = 60;
    private static final long SEARCH_TIMEOUT_MS = 8000L;

    private final MusicState state = MusicState.get();
    private final MusicConfig config;

    private int currentTab = 0;
    private int lastVersion = -1;

    private boolean searching = false;
    private long searchStartedAt = 0L;
    private int searchVersionAtRequest = 0;

    private Button tabPlayerButton;
    private Button tabRequestButton;
    private Button tabPlaylistButton;
    private Button tabSettingsButton;

    private Button playPauseButton;
    private Button stopButton;

    private EditBox searchBox;
    private Button searchButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private SearchResultList resultList;
    private StringWidget pageInfo;

    private PlaylistList playlistList;
    private StringWidget statusInfo;

    private VolumeSlider volumeSlider;
    private Button modeButton;
    private Button hudLyricsButton;
    private PositionSlider hudLyricsXSlider;
    private PositionSlider hudLyricsYSlider;

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
            case 0 -> buildPlayerTab();
            case 1 -> buildRequestTab();
            case 2 -> buildPlaylistTab();
            default -> buildSettingsTab();
        }
    }

    private void buildTabs() {
        int y = 28;
        int w = 90;
        int gap = 6;
        int total = w * 4 + gap * 3;
        int x0 = (width - total) / 2;

        tabPlayerButton = Button.builder(Component.literal("播放界面"), b -> switchTab(0))
                .bounds(x0, y, w, 20).build();
        tabRequestButton = Button.builder(Component.literal("点歌"), b -> switchTab(1))
                .bounds(x0 + w + gap, y, w, 20).build();
        tabPlaylistButton = Button.builder(Component.literal("播放列表"), b -> switchTab(2))
                .bounds(x0 + (w + gap) * 2, y, w, 20).build();
        tabSettingsButton = Button.builder(Component.literal("设置"), b -> switchTab(3))
                .bounds(x0 + (w + gap) * 3, y, w, 20).build();
        addRenderableWidget(tabPlayerButton);
        addRenderableWidget(tabRequestButton);
        addRenderableWidget(tabPlaylistButton);
        addRenderableWidget(tabSettingsButton);

        Component title = Component.literal("WorldmeMusic");
        int titleWidth = font.width(title);
        addRenderableWidget(new StringWidget(width / 2 - titleWidth / 2, 6, titleWidth, 9, title, font));
    }

    private void switchTab(int tab) {
        if (currentTab == tab) {
            return;
        }
        currentTab = tab;
        if (tab == 2) {
            ModNetwork.sendQueueReq();
        }
        rebuild();
    }

    // ---- 播放界面 ----

    private void buildPlayerTab() {
        int rightX = NowPlayingCard.buttonX(width);
        int buttonY = NowPlayingCard.buttonY();
        playPauseButton = Button.builder(PlayerControl.buildPlayPauseLabel(),
                        b -> PlayerControl.togglePlayPause(state))
                .bounds(rightX, buttonY, 80, 20).build();
        stopButton = Button.builder(PlayerControl.buildSecondaryLabel(),
                        b -> PlayerControl.secondaryAction(state))
                .bounds(rightX + 88, buttonY, 80, 20).build();
        addRenderableWidget(playPauseButton);
        addRenderableWidget(stopButton);
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

        resultList = new SearchResultList(minecraft, width, (height - 68) - (rowY + 28),
                rowY + 28, SearchResultList.ROW_HEIGHT);
        resultList.refresh();
        addRenderableWidget(resultList);

        Component pageComponent = buildPageInfo();
        int pageWidth = font.width(pageComponent);
        pageInfo = new StringWidget(width / 2 - pageWidth / 2, height - 66, pageWidth, 9, pageComponent, font);
        addRenderableWidget(pageInfo);

        prevPageButton = Button.builder(Component.literal("上一页"), b -> doPage("prev"))
                .bounds(width / 2 - 130, height - 44, 110, 20).build();
        nextPageButton = Button.builder(Component.literal("下一页"), b -> doPage("next"))
                .bounds(width / 2 + 20, height - 44, 110, 20).build();
        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);
        updateSearchWidgets();
    }

    private void doSearch() {
        if (searching) {
            return;
        }
        String keyword = searchBox.getValue();
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        beginSearch();
        ModNetwork.sendSearch(keyword.trim());
    }

    private void doPage(String action) {
        if (searching) {
            return;
        }
        beginSearch();
        ModNetwork.sendSearchPage(action);
    }

    private void beginSearch() {
        searching = true;
        searchStartedAt = System.currentTimeMillis();
        state.clearSearchError();
        searchVersionAtRequest = state.getSearchVersion();
        updateSearchWidgets();
    }

    private void updateSearchWidgets() {
        if (searchButton != null) {
            searchButton.active = !searching;
            searchButton.setMessage(Component.literal(searching ? searchButtonLabel() : "搜索"));
        }
        if (prevPageButton != null) {
            prevPageButton.active = !searching;
        }
        if (nextPageButton != null) {
            nextPageButton.active = !searching;
        }
    }

    private String searchButtonLabel() {
        long elapsed = System.currentTimeMillis() - searchStartedAt;
        int dots = (int) ((elapsed / 300L) % 3) + 1;
        return "搜索中" + ".".repeat(dots);
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
        Component status = buildStatusInfo();
        int statusWidth = font.width(status);
        statusInfo = new StringWidget(width / 2 - statusWidth / 2, 50, statusWidth, 9, status, font);
        addRenderableWidget(statusInfo);

        playlistList = new PlaylistList(minecraft, width, (height - 68) - PLAYLIST_TOP, PLAYLIST_TOP,
                PlaylistList.ROW_HEIGHT, () -> switchTab(0));
        playlistList.refresh();
        addRenderableWidget(playlistList);
    }

    // ---- 设置页 ----

    private void buildSettingsTab() {
        volumeSlider = new VolumeSlider(width / 2 - 100, 70, 200, 20, config);
        addRenderableWidget(volumeSlider);

        modeButton = Button.builder(buildModeLabel(), b -> toggleMode())
                .bounds(width / 2 - 100, 100, 200, 20).build();
        addRenderableWidget(modeButton);

        Component hintText = WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL
                ? Component.literal("个人模式：自己点歌自己听，不接收全服播放")
                : Component.literal("全服模式：自动接收全服播放，可本地停止当前歌曲");
        int hintWidth = font.width(hintText);
        StringWidget hint = new StringWidget(width / 2 - hintWidth / 2, 126, hintWidth, 9, hintText, font);
        addRenderableWidget(hint);

        hudLyricsButton = Button.builder(buildHudLyricsLabel(), b -> toggleHudLyrics())
                .bounds(width / 2 - 100, 156, 200, 20).build();
        addRenderableWidget(hudLyricsButton);

        hudLyricsXSlider = new PositionSlider(width / 2 - 100, 186, 200, 20, config, true);
        addRenderableWidget(hudLyricsXSlider);

        hudLyricsYSlider = new PositionSlider(width / 2 - 100, 216, 200, 20, config, false);
        addRenderableWidget(hudLyricsYSlider);
    }

    private void toggleHudLyrics() {
        config.setShowHudLyrics(!config.isShowHudLyrics());
        config.save();
        hudLyricsButton.setMessage(buildHudLyricsLabel());
    }

    private Component buildHudLyricsLabel() {
        return Component.literal("游戏内歌词：" + (config.isShowHudLyrics() ? "开" : "关"));
    }

    private void toggleMode() {
        int mode = WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_SERVER
                ? WorldmeMusic.MODE_PERSONAL : WorldmeMusic.MODE_SERVER;
        WorldmeMusic.setPlayMode(mode);
        config.setPlayMode(mode);
        config.save();
        modeButton.setMessage(buildModeLabel());
        state.resetPersonalPlayback();
        state.clearLyrics();
        PersonalPlayback.reset();
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
        if (searching) {
            boolean finished = state.getSearchVersion() != searchVersionAtRequest
                    || System.currentTimeMillis() - searchStartedAt > SEARCH_TIMEOUT_MS;
            if (finished) {
                searching = false;
            }
            updateSearchWidgets();
        }
        if (currentTab == 0) {
            boolean switching = WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL
                    && PersonalPlayback.isSwitching();
            if (playPauseButton != null) {
                playPauseButton.setMessage(PlayerControl.buildPlayPauseLabel());
            }
            if (stopButton != null) {
                stopButton.setMessage(PlayerControl.buildSecondaryLabel());
                stopButton.active = !switching;
            }
        }
        int version = state.getVersion();
        if (version != lastVersion) {
            lastVersion = version;
            if (currentTab == 1 && resultList != null) {
                resultList.refresh();
                if (pageInfo != null) {
                    Component page = buildPageInfo();
                    pageInfo.setMessage(page);
                    int pageWidth = font.width(page);
                    pageInfo.setWidth(pageWidth);
                    pageInfo.setX(width / 2 - pageWidth / 2);
                }
            }
            if (currentTab == 2 && playlistList != null) {
                playlistList.refresh();
                updateStatusInfo();
            }
        }
    }

    private Component buildStatusInfo() {
        String text;
        if (WorldmeMusic.getPlayMode() == WorldmeMusic.MODE_PERSONAL) {
            text = "个人模式：播放列表仅自己可见，点歌自己听";
        } else if (state.isLocalStopped()) {
            text = "已本地停止当前歌曲（仅影响自己）";
        } else {
            text = "全服模式：自动接收全服播放";
        }
        return Component.literal(text);
    }

    private void updateStatusInfo() {
        if (statusInfo == null) {
            return;
        }
        Component text = buildStatusInfo();
        statusInfo.setMessage(text);
        int statusWidth = font.width(text);
        statusInfo.setWidth(statusWidth);
        statusInfo.setX(width / 2 - statusWidth / 2);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractMenuBackground(graphics);
        if (currentTab == 0) {
            NowPlayingCard.render(graphics, font, width, height, state, CoverTextureCache.get());
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (currentTab == 1) {
            String error = state.getSearchError();
            if (!error.isEmpty()) {
                graphics.text(font, error, (width - font.width(error)) / 2, height - 82, 0xFFFF5555);
            }
        }
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
}

package top.worldme.music;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import top.worldme.music.config.MusicConfig;
import top.worldme.music.event.ClientEvent;
import top.worldme.music.gui.WorldmeMusicScreen;
import top.worldme.music.net.ModNetwork;
import top.worldme.music.net.MusicPacketHandler;

/**
 * 客户端初始化：按键绑定、命令、上行 payload、配置加载、消息分发。
 *
 * @author Worldme
 * @since 1.0.0
 */
public class WorldmeMusicClient implements ClientModInitializer {

    public static MusicConfig CONFIG;
    private static boolean volumeApplied;

    @Override
    public void onInitializeClient() {
        ModNetwork.register();

        CONFIG = new MusicConfig();
        CONFIG.load();
        WorldmeMusic.setPlayMode(CONFIG.getPlayMode());
        WorldmeMusicPlayer player = WorldmeMusic.getPlayer();
        if (player != null) {
            player.setVolume(CONFIG.getVolume());
            volumeApplied = true;
        }

        KeyMapping openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.worldmemusic.open", GLFW.GLFW_KEY_M, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!volumeApplied) {
                WorldmeMusicPlayer current = WorldmeMusic.getPlayer();
                if (current != null) {
                    current.setVolume(CONFIG.getVolume());
                    volumeApplied = true;
                }
            }
            while (openKey.consumeClick()) {
                openGui();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("worldmemusic")
                        .executes(context -> {
                            openGui();
                            return 1;
                        })));

        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.MusicPayload.TYPE, (payload, context) ->
                context.client().execute(() -> MusicPacketHandler.handle(payload.str())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEvent.onDisconnect());
    }

    private static void openGui() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.setScreenAndShow(new WorldmeMusicScreen(CONFIG));
        ModNetwork.sendQueueReq();
    }
}
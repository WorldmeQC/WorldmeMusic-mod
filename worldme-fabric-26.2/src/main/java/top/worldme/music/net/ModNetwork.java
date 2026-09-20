package top.worldme.music.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import top.worldme.music.PacketTags;

import java.nio.charset.StandardCharsets;

/**
 * 与 WorldmeMusic-plugin 的客户端上行/下行网络层。
 *
 * <p>通道固定为 {@code zmusic:channel}，报文格式「1 字节前缀 0x9A + UTF-8 文本」。</p>
 *
 * @author Worldme
 * @since 1.0.0
 */
public final class ModNetwork {

    public record MusicPayload(String str) implements CustomPacketPayload {

        public static final Type<MusicPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zmusic", "channel"));

        /** 下行（服务端→客户端）：读取原始字节并剥掉首字节前缀。 */
        public static final StreamCodec<FriendlyByteBuf, MusicPayload> CLIENTBOUND_CODEC =
                CustomPacketPayload.codec((value, buf) -> {
                }, buf -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    if (bytes.length <= 1) {
                        return new MusicPayload("");
                    }
                    byte[] body = new byte[bytes.length - 1];
                    System.arraycopy(bytes, 1, body, 0, body.length);
                    return new MusicPayload(new String(body, StandardCharsets.UTF_8));
                });

        /** 上行（客户端→服务端）：写入 0x9A 前缀 + UTF-8 文本。 */
        public static final StreamCodec<FriendlyByteBuf, MusicPayload> SERVERBOUND_CODEC =
                CustomPacketPayload.codec((value, buf) -> {
                    byte[] body = value.str().getBytes(StandardCharsets.UTF_8);
                    byte[] payload = new byte[body.length + 1];
                    payload[0] = (byte) 0x9A;
                    System.arraycopy(body, 0, payload, 1, body.length);
                    buf.writeBytes(payload);
                }, buf -> new MusicPayload(""));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private ModNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(MusicPayload.TYPE, MusicPayload.CLIENTBOUND_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MusicPayload.TYPE, MusicPayload.SERVERBOUND_CODEC);
    }

    public static boolean canSend() {
        return ClientPlayNetworking.canSend(MusicPayload.TYPE);
    }

    public static void send(String text) {
        if (canSend()) {
            ClientPlayNetworking.send(new MusicPayload(text));
        }
    }

    public static void sendSearch(String keyword) {
        send(PacketTags.SEARCH + keyword);
    }

    public static void sendSearchPage(String action) {
        send(PacketTags.SEARCH_PAGE + action);
    }

    public static void sendAddPub(long songId) {
        send(PacketTags.ADD_PUB + songId);
    }

    public static void sendAddPriv(long songId) {
        send(PacketTags.ADD_PRIV + songId);
    }

    public static void sendPlayPrivateReq(long songId) {
        send(PacketTags.PLAY_PRIV_REQ + songId);
    }

    public static void sendQueueReq() {
        send(PacketTags.QUEUE_REQ);
    }
}
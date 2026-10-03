package com.noteblocksongs.network;

import com.noteblocksongs.NoteBlockSongs;
import com.noteblocksongs.server.SongServerState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public final class SongNetworking {
    public static final int MAX_BYTES = 20 * 1024 * 1024;
    public static final int RANGE = 40;

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(NoteBlockSongs.MOD_ID, path); }

    public record PlayRequest(BlockPos pos, String filename, String hash, byte[] data) implements CustomPayload {
        public static final Type<PlayRequest> TYPE = new Type<>(id("play_request"));
        public static final PacketCodec<RegistryFriendlyByteBuf, PlayRequest> CODEC = PacketCodec.tuple(
                BlockPos.STREAM_CODEC, PlayRequest::pos,
                ByteBufCodecs.stringUtf8(128), PlayRequest::filename,
                ByteBufCodecs.stringUtf8(64), PlayRequest::hash,
                ByteBufCodecs.byteArray(MAX_BYTES), PlayRequest::data,
                PlayRequest::new);
        @Override public Type<? extends CustomPayload> type() { return TYPE; }
    }

    public record StopRequest(BlockPos pos) implements CustomPayload {
        public static final Type<StopRequest> TYPE = new Type<>(id("stop_request"));
        public static final PacketCodec<RegistryFriendlyByteBuf, StopRequest> CODEC = PacketCodec.tuple(BlockPos.STREAM_CODEC, StopRequest::pos, StopRequest::new);
        @Override public Type<? extends CustomPayload> type() { return TYPE; }
    }

    public record SongData(BlockPos pos, String filename, String hash, UUID session, byte[] data) implements CustomPayload {
        public static final Type<SongData> TYPE = new Type<>(id("song_data"));
        public static final PacketCodec<RegistryFriendlyByteBuf, SongData> CODEC = PacketCodec.tuple(
                BlockPos.STREAM_CODEC, SongData::pos,
                ByteBufCodecs.stringUtf8(128), SongData::filename,
                ByteBufCodecs.stringUtf8(64), SongData::hash,
                net.minecraft.network.codec.ByteBufCodecs.UUID, SongData::session,
                ByteBufCodecs.byteArray(MAX_BYTES), SongData::data,
                SongData::new);
        @Override public Type<? extends CustomPayload> type() { return TYPE; }
    }

    public record StopData(BlockPos pos, UUID session) implements CustomPayload {
        public static final Type<StopData> TYPE = new Type<>(id("stop_data"));
        public static final PacketCodec<RegistryFriendlyByteBuf, StopData> CODEC = PacketCodec.tuple(
                BlockPos.STREAM_CODEC, StopData::pos,
                net.minecraft.network.codec.ByteBufCodecs.UUID, StopData::session,
                StopData::new);
        @Override public Type<? extends CustomPayload> type() { return TYPE; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().registerLarge(PlayRequest.TYPE, PlayRequest.CODEC, MAX_BYTES + 1024 * 1024);
        PayloadTypeRegistry.playC2S().register(StopRequest.TYPE, StopRequest.CODEC);
        PayloadTypeRegistry.playS2C().registerLarge(SongData.TYPE, SongData.CODEC, MAX_BYTES + 1024 * 1024);
        PayloadTypeRegistry.playS2C().register(StopData.TYPE, StopData.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PlayRequest.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer sender = context.player();
            if (!validPosition(sender, payload.pos()) || !validFile(payload.filename()) || payload.data().length == 0 || payload.data().length > MAX_BYTES) return;
            String actualHash = sha256(payload.data());
            if (!actualHash.equalsIgnoreCase(payload.hash())) return;

            byte[] data = SongServerState.cached(actualHash);
            if (data == null) {
                data = payload.data();
                SongServerState.cache(actualHash, data);
            }
            UUID session = UUID.randomUUID();
            SongServerState.activate(payload.pos(), session);
            for (ServerPlayer target : sender.serverLevel().players()) {
                if (target.distanceToSqr(payload.pos().getX() + .5, payload.pos().getY() + .5, payload.pos().getZ() + .5) <= RANGE * RANGE
                        && ServerPlayNetworking.canSend(target, SongData.TYPE)) {
                    ServerPlayNetworking.send(target, new SongData(payload.pos(), payload.filename(), actualHash, session, data));
                }
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(StopRequest.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer sender = context.player();
            if (!validPosition(sender, payload.pos())) return;
            UUID session = SongServerState.removeSession(payload.pos());
            if (session == null) return;
            for (ServerPlayer target : sender.serverLevel().players()) {
                if (target.distanceToSqr(payload.pos().getX() + .5, payload.pos().getY() + .5, payload.pos().getZ() + .5) <= RANGE * RANGE
                        && ServerPlayNetworking.canSend(target, StopData.TYPE)) {
                    ServerPlayNetworking.send(target, new StopData(payload.pos(), session));
                }
            }
        }));
    }

    private static boolean validPosition(ServerPlayer player, BlockPos pos) {
        return player.serverLevel().getBlockState(pos).is(net.minecraft.tags.BlockTags.NOTE_BLOCKS)
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 8 * 8;
    }

    private static boolean validFile(String filename) {
        return filename != null && filename.length() <= 128 && filename.toLowerCase(Locale.ROOT).endsWith(".mp3")
                && filename.indexOf('/') < 0 && filename.indexOf('\\') < 0;
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SongData.TYPE, (payload, context) -> context.client().execute(() ->
                com.noteblocksongs.client.audio.PositionalSongPlayer.play(payload.pos(), payload.filename(), payload.hash(), payload.session(), payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(StopData.TYPE, (payload, context) -> context.client().execute(() ->
                com.noteblocksongs.client.audio.PositionalSongPlayer.stop(payload.pos(), payload.session())));
    }

    public static void requestPlay(BlockPos pos, String filename, byte[] data) {
        if (data.length > MAX_BYTES) return;
        ClientPlayNetworking.send(new PlayRequest(pos, filename, sha256(data), data));
    }

    public static void requestStop(BlockPos pos) { ClientPlayNetworking.send(new StopRequest(pos)); }
}

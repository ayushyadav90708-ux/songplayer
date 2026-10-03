package com.noteblocksongs.network;

import com.noteblocksongs.NoteBlockSongs;
import com.noteblocksongs.client.audio.PositionalSongPlayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Path;

public final class SongNetworking {
    private static final int MAX_BYTES = 20 * 1024 * 1024;

    public record PlayRequest(BlockPos pos, String filename, byte[] data) implements CustomPayload {
        public static final CustomPayload.Type<PlayRequest> TYPE =
                new CustomPayload.Type<>(Identifier.fromNamespaceAndPath(NoteBlockSongs.MOD_ID, "play_request"));
        public static final PacketCodec<net.minecraft.network.RegistryFriendlyByteBuf, PlayRequest> CODEC =
                PacketCodec.tuple(
                        BlockPos.STREAM_CODEC, PlayRequest::pos,
                        ByteBufCodecs.STRING_UTF8, PlayRequest::filename,
                        ByteBufCodecs.BYTE_ARRAY, PlayRequest::data,
                        PlayRequest::new);
        @Override public CustomPayload.Type<? extends CustomPayload> type() { return TYPE; }
    }

    public record StopRequest(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Type<StopRequest> TYPE =
                new CustomPayload.Type<>(Identifier.fromNamespaceAndPath(NoteBlockSongs.MOD_ID, "stop_request"));
        public static final PacketCodec<net.minecraft.network.RegistryFriendlyByteBuf, StopRequest> CODEC =
                PacketCodec.tuple(BlockPos.STREAM_CODEC, StopRequest::pos, StopRequest::new);
        @Override public CustomPayload.Type<? extends CustomPayload> type() { return TYPE; }
    }

    public record SongData(BlockPos pos, String filename, byte[] data) implements CustomPayload {
        public static final CustomPayload.Type<SongData> TYPE =
                new CustomPayload.Type<>(Identifier.fromNamespaceAndPath(NoteBlockSongs.MOD_ID, "song_data"));
        public static final PacketCodec<net.minecraft.network.RegistryFriendlyByteBuf, SongData> CODEC =
                PacketCodec.tuple(
                        BlockPos.STREAM_CODEC, SongData::pos,
                        ByteBufCodecs.STRING_UTF8, SongData::filename,
                        ByteBufCodecs.BYTE_ARRAY, SongData::data,
                        SongData::new);
        @Override public CustomPayload.Type<? extends CustomPayload> type() { return TYPE; }
    }

    public record StopData(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Type<StopData> TYPE =
                new CustomPayload.Type<>(Identifier.fromNamespaceAndPath(NoteBlockSongs.MOD_ID, "stop_data"));
        public static final PacketCodec<net.minecraft.network.RegistryFriendlyByteBuf, StopData> CODEC =
                PacketCodec.tuple(BlockPos.STREAM_CODEC, StopData::pos, StopData::new);
        @Override public CustomPayload.Type<? extends CustomPayload> type() { return TYPE; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().registerLarge(PlayRequest.TYPE, PlayRequest.CODEC, MAX_BYTES + 1024 * 1024);
        PayloadTypeRegistry.playC2S().register(StopRequest.TYPE, StopRequest.CODEC);

        // Large payload support is available in modern Fabric API. A 20 MiB application-level
        // limit is still enforced before creating this payload.
        PayloadTypeRegistry.playS2C().registerLarge(SongData.TYPE, SongData.CODEC, MAX_BYTES + 1024 * 1024);
        PayloadTypeRegistry.playS2C().register(StopData.TYPE, StopData.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PlayRequest.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            context.server().execute(() -> {
                if (payload.data() == null || payload.data().length == 0 || payload.data().length > MAX_BYTES) return;
                String filename = payload.filename();
                if (filename == null || filename.isBlank() || filename.length() > 128) return;
                String clean = java.nio.file.Path.of(filename).getFileName().toString();
                if (!clean.equals(filename) || !clean.toLowerCase(java.util.Locale.ROOT).endsWith(".mp3")) return;

                for (ServerPlayer target : sender.serverLevel().players()) {
                    if (target.distanceToSqr(sender) <= 32.0 * 32.0
                            && ServerPlayNetworking.canSend(target, SongData.TYPE)) {
                        ServerPlayNetworking.send(target, new SongData(payload.pos(), clean, payload.data()));
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(StopRequest.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                for (ServerPlayer target : context.player().serverLevel().players()) {
                    if (target.distanceToSqr(context.player()) <= 32.0 * 32.0
                            && ServerPlayNetworking.canSend(target, StopData.TYPE)) {
                        ServerPlayNetworking.send(target, new StopData(payload.pos()));
                    }
                }
            });
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SongData.TYPE, (payload, context) -> {
            context.client().execute(() -> PositionalSongPlayer.play(payload.pos(), payload.filename(), payload.data()));
        });
        ClientPlayNetworking.registerGlobalReceiver(StopData.TYPE, (payload, context) -> {
            context.client().execute(() -> PositionalSongPlayer.stop(payload.pos()));
        });
    }

    public static void requestPlay(BlockPos pos, String filename, byte[] data) {
        ClientPlayNetworking.send(new PlayRequest(pos, filename, data));
    }

    public static void requestStop(BlockPos pos) {
        ClientPlayNetworking.send(new StopRequest(pos));
    }
}

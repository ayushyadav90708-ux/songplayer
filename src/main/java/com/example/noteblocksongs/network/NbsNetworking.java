package com.example.noteblocksongs.network;

import com.example.noteblocksongs.NoteBlockSongs;
import com.example.noteblocksongs.config.NbsConfig;
import com.example.noteblocksongs.server.NbsServerState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public final class NbsNetworking {
    public static final CustomPayload.Id<PlayRequest> PLAY_REQUEST_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"play_request"));
    public static final CustomPayload.Id<UploadStart> UPLOAD_START_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"upload_start"));
    public static final CustomPayload.Id<UploadChunk> UPLOAD_CHUNK_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"upload_chunk"));
    public static final CustomPayload.Id<DownloadRequest> DOWNLOAD_REQUEST_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"download_request"));
    public static final CustomPayload.Id<StopRequest> STOP_REQUEST_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"stop_request"));
    public static final CustomPayload.Id<UploadRequest> UPLOAD_REQUEST_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"upload_request"));
    public static final CustomPayload.Id<UploadAccepted> UPLOAD_ACCEPTED_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"upload_accepted"));
    public static final CustomPayload.Id<PlayStart> PLAY_START_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"play_start"));
    public static final CustomPayload.Id<DownloadChunk> DOWNLOAD_CHUNK_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"download_chunk"));
    public static final CustomPayload.Id<StopS2C> STOP_S2C_ID = new CustomPayload.Id<>(Identifier.of(NoteBlockSongs.MOD_ID,"stop_s2c"));

    private NbsNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(PLAY_REQUEST_ID, PlayRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(UPLOAD_START_ID, UploadStart.CODEC);
        PayloadTypeRegistry.playC2S().registerLarge(UPLOAD_CHUNK_ID, UploadChunk.CODEC, 64_000);
        PayloadTypeRegistry.playC2S().register(DOWNLOAD_REQUEST_ID, DownloadRequest.CODEC);
        PayloadTypeRegistry.playC2S().register(STOP_REQUEST_ID, StopRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(UPLOAD_REQUEST_ID, UploadRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(UPLOAD_ACCEPTED_ID, UploadAccepted.CODEC);
        PayloadTypeRegistry.playS2C().register(PLAY_START_ID, PlayStart.CODEC);
        PayloadTypeRegistry.playS2C().registerLarge(DOWNLOAD_CHUNK_ID, DownloadChunk.CODEC, 64_000);
        PayloadTypeRegistry.playS2C().register(STOP_S2C_ID, StopS2C.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PLAY_REQUEST_ID, (p,c) -> NbsServerState.requestPlay(c.player(),p.pos,p.hash,p.name,p.volume));
        ServerPlayNetworking.registerGlobalReceiver(UPLOAD_START_ID, (p,c) -> NbsServerState.receiveUploadStart(c.player(),p.hash,p.name,p.size));
        ServerPlayNetworking.registerGlobalReceiver(UPLOAD_CHUNK_ID, (p,c) -> NbsServerState.receiveUploadChunk(c.player(),p.hash,p.index,p.data,p.total));
        ServerPlayNetworking.registerGlobalReceiver(DOWNLOAD_REQUEST_ID, (p,c) -> NbsServerState.requestDownload(c.player(),p.hash));
        ServerPlayNetworking.registerGlobalReceiver(STOP_REQUEST_ID, (p,c) -> NbsServerState.stop(c.player(),p.pos));
    }

    public static boolean canSend(ServerPlayerEntity p) { return ServerPlayNetworking.canSend(p, PLAY_START_ID); }
    public static void sendUploadRequest(ServerPlayerEntity p,String hash,String name){ServerPlayNetworking.send(p,new UploadRequest(hash,name));}
    public static void sendUploadAccepted(ServerPlayerEntity p,String hash){ServerPlayNetworking.send(p,new UploadAccepted(hash));}
    public static void sendPlayStart(ServerPlayerEntity p,NbsServerState.ActiveSource s,int range){ServerPlayNetworking.send(p,new PlayStart(s.pos(),s.hash(),s.name(),s.volume(),range));}
    public static void sendDownloadChunk(ServerPlayerEntity p,String hash,int index,int total,byte[] data){ServerPlayNetworking.send(p,new DownloadChunk(hash,index,total,data));}
    public static void sendStop(ServerPlayerEntity p,BlockPos pos){ServerPlayNetworking.send(p,new StopS2C(pos));}

    public record PlayRequest(BlockPos pos,String hash,String name,float volume) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,PlayRequest> CODEC=PacketCodec.of(PlayRequest::write,PlayRequest::new);
        private PlayRequest(RegistryByteBuf b){this(b.readBlockPos(),b.readString(80),b.readString(160),b.readFloat());}
        private void write(RegistryByteBuf b){b.writeBlockPos(pos);b.writeString(hash,64);b.writeString(name,160);b.writeFloat(volume);}
        public Id<? extends CustomPayload> getId(){return PLAY_REQUEST_ID;}
    }
    public record UploadStart(String hash,String name,int size) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,UploadStart> CODEC=PacketCodec.of(UploadStart::write,UploadStart::new);
        private UploadStart(RegistryByteBuf b){this(b.readString(64),b.readString(160),b.readInt());}
        private void write(RegistryByteBuf b){b.writeString(hash,64);b.writeString(name,160);b.writeInt(size);}
        public Id<? extends CustomPayload> getId(){return UPLOAD_START_ID;}
    }
    public record UploadChunk(String hash,int index,int total,byte[] data) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,UploadChunk> CODEC=PacketCodec.of(UploadChunk::write,UploadChunk::new);
        private UploadChunk(RegistryByteBuf b){this(b.readString(64),b.readInt(),b.readInt(),b.readByteArray(NbsConfig.INSTANCE.upload_chunk_bytes));}
        private void write(RegistryByteBuf b){b.writeString(hash,64);b.writeInt(index);b.writeInt(total);b.writeByteArray(data);}
        public Id<? extends CustomPayload> getId(){return UPLOAD_CHUNK_ID;}
    }
    public record DownloadRequest(String hash) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,DownloadRequest> CODEC=PacketCodec.of(DownloadRequest::write,DownloadRequest::new);
        private DownloadRequest(RegistryByteBuf b){this(b.readString(64));}
        private void write(RegistryByteBuf b){b.writeString(hash,64);}
        public Id<? extends CustomPayload> getId(){return DOWNLOAD_REQUEST_ID;}
    }
    public record StopRequest(BlockPos pos) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,StopRequest> CODEC=PacketCodec.of(StopRequest::write,StopRequest::new);
        private StopRequest(RegistryByteBuf b){this(b.readBlockPos());}
        private void write(RegistryByteBuf b){b.writeBlockPos(pos);}
        public Id<? extends CustomPayload> getId(){return STOP_REQUEST_ID;}
    }
    public record UploadRequest(String hash,String name) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,UploadRequest> CODEC=PacketCodec.of(UploadRequest::write,UploadRequest::new);
        private UploadRequest(RegistryByteBuf b){this(b.readString(64),b.readString(160));}
        private void write(RegistryByteBuf b){b.writeString(hash,64);b.writeString(name,160);}
        public Id<? extends CustomPayload> getId(){return UPLOAD_REQUEST_ID;}
    }
    public record UploadAccepted(String hash) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,UploadAccepted> CODEC=PacketCodec.of(UploadAccepted::write,UploadAccepted::new);
        private UploadAccepted(RegistryByteBuf b){this(b.readString(64));}
        private void write(RegistryByteBuf b){b.writeString(hash,64);}
        public Id<? extends CustomPayload> getId(){return UPLOAD_ACCEPTED_ID;}
    }
    public record PlayStart(BlockPos pos,String hash,String name,float volume,int range) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,PlayStart> CODEC=PacketCodec.of(PlayStart::write,PlayStart::new);
        private PlayStart(RegistryByteBuf b){this(b.readBlockPos(),b.readString(64),b.readString(160),b.readFloat(),b.readVarInt());}
        private void write(RegistryByteBuf b){b.writeBlockPos(pos);b.writeString(hash,64);b.writeString(name,160);b.writeFloat(volume);b.writeVarInt(range);}
        public Id<? extends CustomPayload> getId(){return PLAY_START_ID;}
    }
    public record DownloadChunk(String hash,int index,int total,byte[] data) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,DownloadChunk> CODEC=PacketCodec.of(DownloadChunk::write,DownloadChunk::new);
        private DownloadChunk(RegistryByteBuf b){this(b.readString(64),b.readInt(),b.readInt(),b.readByteArray(NbsConfig.INSTANCE.upload_chunk_bytes));}
        private void write(RegistryByteBuf b){b.writeString(hash,64);b.writeInt(index);b.writeInt(total);b.writeByteArray(data);}
        public Id<? extends CustomPayload> getId(){return DOWNLOAD_CHUNK_ID;}
    }
    public record StopS2C(BlockPos pos) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf,StopS2C> CODEC=PacketCodec.of(StopS2C::write,StopS2C::new);
        private StopS2C(RegistryByteBuf b){this(b.readBlockPos());}
        private void write(RegistryByteBuf b){b.writeBlockPos(pos);}
        public Id<? extends CustomPayload> getId(){return STOP_S2C_ID;}
    }
}

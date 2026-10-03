package com.example.noteblocksongs.server;

import com.example.noteblocksongs.config.NbsConfig;
import com.example.noteblocksongs.network.NbsNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NbsServerState {
    private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ActiveSource> SOURCES = new HashMap<>();
    private static final Map<UUID, UploadSession> UPLOADS = new HashMap<>();
    private static final Map<UUID, Long> RATE_START = new HashMap<>();
    private static final Map<UUID, Long> RATE_BYTES = new HashMap<>();
    private static MinecraftServer server;

    private NbsServerState() {}

    public static void init(MinecraftServer s) {
        server = s;
        loadCache();
    }

    public static void tick(MinecraftServer s) {
        if (server != s) init(s);
        if (s.getTicks() % 20 != 0) return;
        SOURCES.entrySet().removeIf(entry -> {
            ActiveSource src = entry.getValue();
            if (!src.world.isChunkLoaded(src.pos.getX() >> 4, src.pos.getZ() >> 4) || !src.world.getBlockState(src.pos).isOf(Blocks.NOTE_BLOCK)) {
                broadcastStop(src);
                return true;
            }
            return false;
        });
    }

    public static synchronized byte[] getCached(String hash) { return CACHE.get(hash); }

    public static synchronized boolean cache(String hash, byte[] data) {
        CACHE.put(hash, data);
        try {
            Path dir = cacheDir(); Files.createDirectories(dir);
            Files.write(dir.resolve(hash + ".mp3"), data);
            trimCache();
            return true;
        } catch (IOException e) { CACHE.remove(hash); return false; }
    }

    public static synchronized void requestPlay(ServerPlayerEntity player, BlockPos pos, String hash, String name, float volume) {
        ServerWorld world = player.getServerWorld();
        if (!world.getBlockState(pos).isOf(Blocks.NOTE_BLOCK)) return;
        if (player.squaredDistanceTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) > (NbsConfig.INSTANCE.max_playback_range + 4L) * (NbsConfig.INSTANCE.max_playback_range + 4L)) return;
        if (hash.length() != 64 || !hash.matches("[0-9a-fA-F]+")) return;
        if (SOURCES.size() >= NbsConfig.INSTANCE.max_active_sources && !SOURCES.containsKey(key(world, pos))) return;
        byte[] data = CACHE.get(hash.toLowerCase(Locale.ROOT));
        if (data == null) {
            NbsNetworking.sendUploadRequest(player, hash.toLowerCase(Locale.ROOT), name);
            return;
        }
        start(world, pos, hash.toLowerCase(Locale.ROOT), name, volume);
    }

    public static synchronized void receiveUploadStart(ServerPlayerEntity player, String hash, String name, int size) {
        if (hash.length() != 64 || size <= 0 || size > NbsConfig.INSTANCE.max_file_size_mb * 1024 * 1024) return;
        if (CACHE.containsKey(hash)) { startAfterUpload(player, hash, name); return; }
        UPLOADS.put(player.getUuid(), new UploadSession(hash, name, size));
    }

    public static synchronized void receiveUploadChunk(ServerPlayerEntity player, String hash, int index, byte[] data, int totalChunks) {
        UploadSession s = UPLOADS.get(player.getUuid());
        if (s == null || !s.hash.equals(hash) || index < 0 || index >= totalChunks || totalChunks <= 0 || totalChunks > 20_000) return;
        if (data.length > NbsConfig.INSTANCE.upload_chunk_bytes) return;
        if (!rateOk(player, data.length)) { UPLOADS.remove(player.getUuid()); return; }
        if (s.totalChunks != 0 && s.totalChunks != totalChunks) return;
        s.totalChunks = totalChunks;
        s.chunks.put(index, data);
        s.bytes += data.length;
        if (s.bytes > s.size || s.chunks.size() == s.totalChunks) {
            if (s.bytes != s.size || s.chunks.size() != s.totalChunks) { UPLOADS.remove(player.getUuid()); return; }
            byte[] joined = join(s);
            if (!Mp3DurationGuard.withinLimit(joined, NbsConfig.INSTANCE.max_duration_seconds)) { UPLOADS.remove(player.getUuid()); return; }
            if (!sha256(joined).equalsIgnoreCase(s.hash)) { UPLOADS.remove(player.getUuid()); return; }
            if (!cache(s.hash, joined)) { UPLOADS.remove(player.getUuid()); return; }
            UPLOADS.remove(player.getUuid());
            startAfterUpload(player, s.hash, s.name);
        }
    }

    private static boolean rateOk(ServerPlayerEntity player, int amount) {
        long now = System.currentTimeMillis();
        UUID id = player.getUuid();
        long start = RATE_START.getOrDefault(id, now);
        long used = RATE_BYTES.getOrDefault(id, 0L);
        if (now - start >= 1000) { start = now; used = 0; }
        used += amount;
        RATE_START.put(id, start); RATE_BYTES.put(id, used);
        return used <= NbsConfig.INSTANCE.max_transfer_bytes_per_second;
    }

    public static synchronized void requestDownload(ServerPlayerEntity player, String hash) {
        byte[] data = CACHE.get(hash);
        if (data == null || data.length > NbsConfig.INSTANCE.max_file_size_mb * 1024 * 1024) return;
        int chunk = NbsConfig.INSTANCE.upload_chunk_bytes;
        int total = (data.length + chunk - 1) / chunk;
        for (int i=0;i<total;i++) {
            int from=i*chunk, to=Math.min(data.length, from+chunk);
            NbsNetworking.sendDownloadChunk(player, hash, i, total, Arrays.copyOfRange(data, from, to));
        }
    }

    public static synchronized void stop(ServerPlayerEntity player, BlockPos pos) {
        String k = key(player.getServerWorld(), pos);
        ActiveSource src = SOURCES.remove(k);
        if (src != null) broadcastStop(src);
    }

    private static void startAfterUpload(ServerPlayerEntity player, String hash, String name) {
        NbsNetworking.sendUploadAccepted(player, hash);
    }

    public static synchronized void confirmUploadedPlay(ServerPlayerEntity player, String hash, String name, BlockPos pos) {
        byte[] data = CACHE.get(hash);
        if (data == null) return;
        start(player.getServerWorld(), pos, hash, name, 1.0f);
    }

    private static void start(ServerWorld world, BlockPos pos, String hash, String name, float volume) {
        String k=key(world,pos);
        ActiveSource old=SOURCES.put(k,new ActiveSource(world,pos,hash,name,Math.max(0,Math.min(2,volume))));
        if(old!=null) broadcastStop(old);
        ActiveSource src=SOURCES.get(k);
        for (ServerPlayerEntity p : PlayerLookup.around(world, pos, NbsConfig.INSTANCE.max_playback_range)) {
            if (NbsNetworking.canSend(p)) NbsNetworking.sendPlayStart(p, src, NbsConfig.INSTANCE.max_playback_range);
        }
    }

    private static void broadcastStop(ActiveSource src) {
        for (ServerPlayerEntity p : PlayerLookup.around(src.world, src.pos, NbsConfig.INSTANCE.max_playback_range)) NbsNetworking.sendStop(p, src.pos);
    }

    private static String key(ServerWorld w, BlockPos p) { return w.getRegistryKey().getValue()+"|"+p.asLong(); }
    private static Path cacheDir() { return server.getRunDirectory().resolve("note_block_songs_cache"); }
    private static void loadCache() {
        try { Files.createDirectories(cacheDir());
            try (var stream=Files.list(cacheDir())) { stream.filter(p->p.getFileName().toString().endsWith(".mp3")).forEach(p->{ try { String h=p.getFileName().toString().replace(".mp3",""); if(h.matches("[0-9a-fA-F]{64}")) CACHE.put(h,Files.readAllBytes(p)); } catch(IOException ignored){} }); }
        } catch(IOException ignored){}
    }
    private static void trimCache() {
        long max=(long)NbsConfig.INSTANCE.max_cache_mb*1024*1024;
        long total=CACHE.values().stream().mapToLong(a->a.length).sum();
        if(total<=max)return;
        Iterator<Map.Entry<String,byte[]>> it=CACHE.entrySet().iterator();
        while(total>max && it.hasNext()){ var e=it.next(); total-=e.getValue().length; try{Files.deleteIfExists(cacheDir().resolve(e.getKey()+".mp3"));}catch(IOException ignored){} it.remove(); }
    }
    private static byte[] join(UploadSession s) { byte[] out=new byte[s.size]; int at=0; for(int i=0;i<s.totalChunks;i++){byte[] c=s.chunks.get(i);System.arraycopy(c,0,out,at,c.length);at+=c.length;} return out; }
    public static String sha256(byte[] d){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(d);StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}

    public record ActiveSource(ServerWorld world, BlockPos pos, String hash, String name, float volume) {}
    private static final class UploadSession { final String hash,name; final int size; int totalChunks; int bytes; final Map<Integer,byte[]> chunks=new HashMap<>(); UploadSession(String h,String n,int s){hash=h;name=n;size=s;} }
}

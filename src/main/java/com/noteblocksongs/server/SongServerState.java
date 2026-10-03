package com.noteblocksongs.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.noteblocksongs.network.SongNetworking;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SongServerState {
    private static final int MAX_CACHE_ENTRIES = 5;
    private static final Map<String, byte[]> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) { return size() > MAX_CACHE_ENTRIES; }
    };
    private static final Map<BlockPos, UUID> ACTIVE = new java.util.HashMap<>();
    private SongServerState() {}

    public static synchronized byte[] cached(String hash) { return CACHE.get(hash); }
    public static synchronized void cache(String hash, byte[] data) { CACHE.put(hash, data); }
    public static synchronized void activate(BlockPos pos, UUID session) { ACTIVE.put(pos.immutable(), session); }
    public static synchronized UUID removeSession(BlockPos pos) { return ACTIVE.remove(pos); }

    public static void stopAt(MinecraftServer server, ServerLevel level, BlockPos pos) {
        UUID session = removeSession(pos);
        if (session == null) return;
        for (ServerPlayer target : level.players()) {
            if (target.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= SongNetworking.RANGE * SongNetworking.RANGE
                    && ServerPlayNetworking.canSend(target, SongNetworking.StopData.TYPE)) {
                ServerPlayNetworking.send(target, new SongNetworking.StopData(pos, session));
            }
        }
    }
}

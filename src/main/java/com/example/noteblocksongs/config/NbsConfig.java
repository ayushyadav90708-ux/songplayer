package com.example.noteblocksongs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NbsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final NbsConfig INSTANCE = new NbsConfig();

    public int max_file_size_mb = 16;
    public int max_duration_seconds = 600;
    public int max_transfer_bytes_per_second = 2_000_000;
    public int max_active_sources = 8;
    public int max_playback_range = 64;
    public int max_cache_mb = 512;
    public float volume = 1.0f;
    public int upload_chunk_bytes = 48_000;

    private NbsConfig() {}

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("note_block_songs.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                NbsConfig loaded = GSON.fromJson(Files.readString(p), NbsConfig.class);
                if (loaded != null) copy(loaded);
            } else save();
        } catch (Exception ignored) {
            save();
        }
    }

    private static void copy(NbsConfig c) {
        INSTANCE.max_file_size_mb = clamp(c.max_file_size_mb, 1, 64);
        INSTANCE.max_duration_seconds = clamp(c.max_duration_seconds, 10, 1800);
        INSTANCE.max_transfer_bytes_per_second = clamp(c.max_transfer_bytes_per_second, 64_000, 10_000_000);
        INSTANCE.max_active_sources = clamp(c.max_active_sources, 1, 32);
        INSTANCE.max_playback_range = clamp(c.max_playback_range, 8, 128);
        INSTANCE.max_cache_mb = clamp(c.max_cache_mb, 16, 4096);
        INSTANCE.volume = Math.max(0.0f, Math.min(2.0f, c.volume));
        INSTANCE.upload_chunk_bytes = clamp(c.upload_chunk_bytes, 8_000, 48_000);
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(INSTANCE));
        } catch (IOException ignored) {}
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}

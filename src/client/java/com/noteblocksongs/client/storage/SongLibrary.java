package com.noteblocksongs.client.storage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class SongLibrary {
    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static Path songsDir;
    private SongLibrary() {}

    public static void init() {
        songsDir = FabricLoader.getInstance().getGameDir().resolve("Songs");
        try { Files.createDirectories(songsDir); }
        catch (IOException e) { System.err.println("[NoteBlockSongs] Could not create Songs folder: " + e); }
    }

    public static Path directory() { if (songsDir == null) init(); return songsDir; }

    public static List<Path> scan() {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3"))
                    .filter(p -> { try { return Files.size(p) <= MAX_FILE_BYTES; } catch (IOException e) { return false; } })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(result::add);
        } catch (IOException e) { System.err.println("[NoteBlockSongs] Songs scan failed: " + e); }
        return result;
    }

    public static byte[] read(Path path) throws IOException {
        Path safe = path.toAbsolutePath().normalize();
        Path base = directory().toAbsolutePath().normalize();
        if (!safe.startsWith(base) || !safe.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3")) throw new IOException("Invalid song path");
        if (Files.size(safe) > MAX_FILE_BYTES) throw new IOException("Song exceeds 20 MiB");
        return Files.readAllBytes(safe);
    }
}

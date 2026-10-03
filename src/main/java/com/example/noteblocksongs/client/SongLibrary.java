package com.example.noteblocksongs.client;

import com.example.noteblocksongs.NoteBlockSongs;
import com.example.noteblocksongs.server.NbsServerState;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public final class SongLibrary {
    private static List<Song> songs = List.of();

    public static Path songsDir() { return FabricLoader.getInstance().getGameDir().resolve("Songs"); }
    public static Path cacheDir() { return songsDir().resolve(".cache"); }

    public static void refresh() {
        try {
            Files.createDirectories(songsDir());
            Files.createDirectories(cacheDir());
            List<Song> found = new ArrayList<>();
            try (var stream=Files.list(songsDir())) {
                stream.filter(Files::isRegularFile).filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3"))
                    .sorted(Comparator.comparing(p->p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(p->{try{long size=Files.size(p); if(size>0) found.add(new Song(p.getFileName().toString(),p,hash(p),size));}catch(Exception ignored){}});
            }
            songs=List.copyOf(found);
        } catch(IOException ignored) { songs=List.of(); }
    }

    public static List<Song> songs(){return songs;}
    public static Song find(String hash){return songs.stream().filter(s->s.hash.equalsIgnoreCase(hash)).findFirst().orElse(null);}
    public static byte[] read(Song s)throws IOException{return Files.readAllBytes(s.path);}
    public static Path cachePath(String hash){return cacheDir().resolve(hash.toLowerCase(Locale.ROOT)+".mp3");}
    public static boolean hasCached(String hash){return Files.isRegularFile(cachePath(hash));}
    public static void writeCached(String hash,byte[] data){try{Files.createDirectories(cacheDir());Files.write(cachePath(hash),data,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);}catch(IOException ignored){}}
    public static byte[] readCached(String hash)throws IOException{return Files.readAllBytes(cachePath(hash));}
    public static String hash(Path p)throws Exception{return NoteBlockSongsHash.sha256(Files.readAllBytes(p));}
    public record Song(String name,Path path,String hash,long size){}

    private static final class NoteBlockSongsHash {
        static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder b=new StringBuilder(64);for(byte x:h)b.append(String.format("%02x",x));return b.toString();}
    }
}

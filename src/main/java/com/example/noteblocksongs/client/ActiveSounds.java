package com.example.noteblocksongs.client;

import com.example.noteblocksongs.audio.NoteBlockMp3Sound;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveSounds {
    private static final Map<Long, SoundInstance> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<Long, String> NAMES = new ConcurrentHashMap<>();

    public static void play(BlockPos pos, byte[] data, float volume) {
        MinecraftClient mc=MinecraftClient.getInstance();
        stop(pos);
        NoteBlockMp3Sound sound=new NoteBlockMp3Sound(pos,data,volume);
        ACTIVE.put(pos.asLong(),sound);
        mc.getSoundManager().play(sound);
    }

    public static void setName(BlockPos pos, String name){NAMES.put(pos.asLong(),name);}
    public static String name(BlockPos pos){return NAMES.getOrDefault(pos.asLong(),"Nothing playing");}

    public static void stop(BlockPos pos) {
        SoundInstance s=ACTIVE.remove(pos.asLong());
        if(s!=null) MinecraftClient.getInstance().getSoundManager().stop(s); NAMES.remove(pos.asLong());
    }
}

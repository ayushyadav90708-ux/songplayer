package com.example.noteblocksongs.audio;

import com.example.noteblocksongs.NoteBlockSongs;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundBufferLibrary;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class NoteBlockMp3Sound extends AbstractSoundInstance implements FabricSoundInstance {
    private final byte[] data;
    private final float localVolume;

    public NoteBlockMp3Sound(BlockPos pos, byte[] data, float volume) {
        super(net.minecraft.sound.SoundEvent.of(NoteBlockSongs.MP3_SOUND_ID), SoundCategory.RECORDS, createRandom());
        this.data=data;
        this.localVolume=Math.max(0,Math.min(2,volume));
        this.x=pos.getX()+0.5; this.y=pos.getY()+0.5; this.z=pos.getZ()+0.5;
        this.repeat=false;
        this.relative=false;
        this.attenuationType=SoundInstance.AttenuationType.LINEAR;
        this.volume=localVolume;
        this.pitch=1.0f;
    }

    @Override public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
        try { return CompletableFuture.completedFuture(new Mp3AudioStream(data)); }
        catch(IOException e){ return CompletableFuture.failedFuture(e); }
    }
}

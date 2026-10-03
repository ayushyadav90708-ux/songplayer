package com.noteblocksongs.client.audio;

import javazoom.jl.decoder.*;
import org.lwjgl.openal.AL10;
import org.lwjgl.BufferUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PositionalSongPlayer {
    private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Active> ACTIVE = new HashMap<>();
    private static final float DEFAULT_VOLUME = 1.0f;
    private static final float MAX_DISTANCE = 32.0f;

    private PositionalSongPlayer() {}

    public static void init() {}

    public static void play(BlockPos pos, String filename, byte[] mp3) {
        stop(pos);
        CACHE.put(filename, mp3);

        try {
            Pcm pcm = decode(mp3);
            int buffer = AL10.alGenBuffers();
            int source = AL10.alGenSources();

            ByteBuffer data = BufferUtils.createByteBuffer(pcm.bytes.length);
            data.put(pcm.bytes).flip();

            int format = pcm.channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            AL10.alBufferData(buffer, format, data, pcm.sampleRate);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, DEFAULT_VOLUME);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 4.0f);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, MAX_DISTANCE);
            AL10.alSource3f(source, AL10.AL_POSITION, pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f);
            AL10.alSourcePlay(source);

            ACTIVE.put(pos, new Active(source, buffer));
        } catch (Exception ignored) {}
    }

    public static void stop(BlockPos pos) {
        Active active = ACTIVE.remove(pos);
        if (active != null) {
            AL10.alSourceStop(active.source);
            AL10.alDeleteSources(active.source);
            AL10.alDeleteBuffers(active.buffer);
        }
    }

    public static void tick(Minecraft client) {
        Iterator<Map.Entry<BlockPos, Active>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Active> e = it.next();
            if (AL10.alGetSourcei(e.getValue().source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(e.getValue().source);
                AL10.alDeleteBuffers(e.getValue().buffer);
                it.remove();
            }
        }
    }

    public static void shutdown() {
        for (BlockPos pos : new ArrayList<>(ACTIVE.keySet())) stop(pos);
        CACHE.clear();
    }

    private record Active(int source, int buffer) {}

    private record Pcm(byte[] bytes, int sampleRate, int channels) {}

    private static Pcm decode(byte[] mp3) throws Exception {
        Bitstream stream = new Bitstream(new ByteArrayInputStream(mp3));
        Decoder decoder = new Decoder();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        Header header;
        int sampleRate = 44100;
        int channels = 2;

        while ((header = stream.readFrame()) != null) {
            SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, stream);
            sampleRate = samples.getSampleFrequency();
            channels = samples.getChannelCount();

            short[] buffer = samples.getBuffer();
            int len = samples.getBufferLength();
            for (int i = 0; i < len; i++) {
                short s = buffer[i];
                out.write(s & 0xFF);
                out.write((s >>> 8) & 0xFF);
            }
            stream.closeFrame();
        }
        stream.close();
        return new Pcm(out.toByteArray(), sampleRate, channels);
    }
}

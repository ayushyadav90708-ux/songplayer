package com.noteblocksongs.client.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.lwjgl.openal.AL10;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PositionalSongPlayer {
    private static final Map<BlockPos, Playback> ACTIVE = new ConcurrentHashMap<>();
    private static float volume = 1.0f;
    private PositionalSongPlayer() {}

    public static void init() {}

    public static void play(BlockPos pos, String filename, String hash, UUID session, byte[] mp3) {
        stop(pos, null);
        try {
            Decoded decoded = decode(mp3);
            Playback playback = new Playback(pos.immutable(), hash, session, decoded);
            playback.start();
            ACTIVE.put(pos.immutable(), playback);
        } catch (Throwable t) {
            System.err.println("[NoteBlockSongs] Could not play " + filename + ": " + t);
        }
    }

    public static void stop(BlockPos pos, UUID session) {
        Playback p = ACTIVE.get(pos);
        if (p == null) return;
        if (session != null && !session.equals(p.session)) return;
        ACTIVE.remove(pos);
        p.close();
    }

    public static void tick(Minecraft client) {
        if (client.player == null) return;
        for (Playback p : ACTIVE.values()) p.tick();
    }

    public static void setVolume(float value) {
        volume = Math.max(0f, Math.min(1f, value));
        for (Playback p : ACTIVE.values()) p.applyGain();
    }
    public static float volume() { return volume; }

    public static void shutdown() {
        for (Playback p : ACTIVE.values()) p.close();
        ACTIVE.clear();
    }

    private record Decoded(byte[] pcm, int sampleRate, int channels) {}

    private static Decoded decode(byte[] mp3) throws Exception {
        Bitstream stream = new Bitstream(new ByteArrayInputStream(mp3));
        Decoder decoder = new Decoder();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(Math.min(mp3.length * 8, 64 * 1024 * 1024));
        int rate = 44100, channels = 2;
        try {
            Header h;
            while ((h = stream.readFrame()) != null) {
                SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(h, stream);
                rate = buffer.getSampleFrequency();
                channels = buffer.getChannelCount();
                short[] samples = buffer.getBuffer();
                int count = buffer.getBufferLength();
                byte[] bytes = new byte[count * 2];
                for (int i = 0; i < count; i++) {
                    short s = samples[i];
                    bytes[i * 2] = (byte) (s & 0xff);
                    bytes[i * 2 + 1] = (byte) ((s >>> 8) & 0xff);
                }
                out.write(bytes);
                stream.closeFrame();
            }
        } finally { stream.close(); }
        return new Decoded(out.toByteArray(), rate, channels);
    }

    private static final class Playback {
        final BlockPos pos; final String hash; final UUID session; final Decoded decoded;
        int buffer = 0, source = 0;
        Playback(BlockPos pos, String hash, UUID session, Decoded decoded) { this.pos = pos; this.hash = hash; this.session = session; this.decoded = decoded; }

        void start() {
            int format = decoded.channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            buffer = AL10.alGenBuffers();
            source = AL10.alGenSources();
            ByteBuffer pcm = ByteBuffer.allocateDirect(decoded.pcm.length).order(ByteOrder.nativeOrder());
            pcm.put(decoded.pcm).flip();
            AL10.alBufferData(buffer, format, pcm, decoded.sampleRate);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 3.0f);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1.0f);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 40.0f);
            applyGain();
            updatePosition();
            AL10.alSourcePlay(source);
        }

        void applyGain() { if (source != 0) AL10.alSourcef(source, AL10.AL_GAIN, volume); }
        void updatePosition() { if (source != 0) AL10.alSource3f(source, AL10.AL_POSITION, pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f); }
        void tick() {
            updatePosition();
            if (source != 0 && AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED) close();
        }
        void close() {
            if (source != 0) { AL10.alSourceStop(source); AL10.alDeleteSources(source); source = 0; }
            if (buffer != 0) { AL10.alDeleteBuffers(buffer); buffer = 0; }
        }
    }
}

package com.example.noteblocksongs.server;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;

import java.io.ByteArrayInputStream;

/** Conservative MP3 duration guard. It reads frame headers and stops early if the configured limit is exceeded. */
public final class Mp3DurationGuard {
    private Mp3DurationGuard() {}
    public static boolean withinLimit(byte[] mp3, int maxSeconds) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(mp3)) {
            Bitstream bs = new Bitstream(in);
            long ms = 0;
            int frames = 0;
            Header h;
            while ((h = bs.readFrame()) != null) {
                // JLayer's header exposes milliseconds per MPEG frame.
                ms += Math.max(1, h.ms_per_frame());
                frames++;
                bs.closeFrame();
                if (ms > maxSeconds * 1000L) { bs.close(); return false; }
                if (frames > 1_000_000) { bs.close(); return false; }
            }
            bs.close();
            return frames > 0;
        } catch (Throwable t) {
            // Invalid/unsupported MP3s are rejected rather than allowing a bypass.
            return false;
        }
    }
}

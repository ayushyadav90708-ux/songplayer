package com.example.noteblocksongs.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sound.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Mp3AudioStream implements AudioStream {
    private final InputStream input;
    private final Bitstream bitstream;
    private final Decoder decoder;
    private final AudioFormat format;
    private SampleBuffer current;
    private int sampleIndex;
    private byte[] pending = new byte[0];
    private int pendingPos;
    private boolean eof;

    public Mp3AudioStream(byte[] mp3) throws IOException {
        this.input = new ByteArrayInputStream(mp3);
        this.bitstream = new Bitstream(input);
        this.decoder = new Decoder();
        try {
            Header h = bitstream.readFrame();
            if (h == null) throw new IOException("Empty MP3");
            current = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            sampleIndex = 0;
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, current.getSampleFrequency(), 16, current.getChannelCount(), current.getChannelCount()*2, current.getSampleFrequency(), false);
            bitstream.closeFrame();
        } catch (Exception e) {
            close();
            throw new IOException("Unable to decode MP3", e);
        }
    }

    @Override public AudioFormat getFormat(){return format;}

    @Override public synchronized ByteBuffer read(int size) throws IOException {
        if(size<=0) return ByteBuffer.allocate(0);
        ByteBuffer out=ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        while(out.remaining()>=2){
            if(current!=null && sampleIndex<current.getBufferLength()){
                short[] buf=current.getBuffer();
                while(sampleIndex<current.getBufferLength() && out.remaining()>=2){out.putShort(buf[sampleIndex++]);}
                continue;
            }
            if(eof) break;
            try {
                Header h=bitstream.readFrame();
                if(h==null){eof=true;break;}
                current=(SampleBuffer)decoder.decodeFrame(h,bitstream);
                sampleIndex=0;
                bitstream.closeFrame();
            } catch (BitstreamException e){eof=true;break;} catch(Exception e){throw new IOException("MP3 decode error",e);}
        }
        out.flip();
        return out;
    }

    @Override public void close() throws IOException { try{bitstream.close();}catch(Exception ignored){} input.close(); }
}

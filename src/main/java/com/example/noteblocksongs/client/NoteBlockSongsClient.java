package com.example.noteblocksongs.client;

import com.example.noteblocksongs.network.NbsNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class NoteBlockSongsClient implements ClientModInitializer {
    private static final Map<String, DownloadAssembly> DOWNLOADS = new HashMap<>();
    private static final Map<String, PendingPlay> PENDING = new HashMap<>();

    @Override public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> SongLibrary.refresh());
        UseBlockCallback.EVENT.register((player,world,hand,hit) -> {
            if(world.getBlockState(hit.getBlockPos()).isOf(Blocks.NOTE_BLOCK)) {
                MinecraftClient.getInstance().setScreen(new SongsScreen(hit.getBlockPos()));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        ClientPlayNetworking.registerGlobalReceiver(NbsNetworking.UPLOAD_REQUEST_ID, (payload, ctx) -> ctx.client().execute(() -> handleUploadRequest(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NbsNetworking.UPLOAD_ACCEPTED_ID, (payload, ctx) -> ctx.client().execute(() -> handleUploadAccepted(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NbsNetworking.PLAY_START_ID, (payload, ctx) -> ctx.client().execute(() -> handlePlayStart(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NbsNetworking.DOWNLOAD_CHUNK_ID, (payload, ctx) -> ctx.client().execute(() -> handleDownloadChunk(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NbsNetworking.STOP_S2C_ID, (payload, ctx) -> ctx.client().execute(() -> ActiveSounds.stop(payload.pos())));
    }

    public static void requestPlay(BlockPos pos, SongLibrary.Song song, float volume) {
        PENDING.put(song.hash(),new PendingPlay(pos,song.hash(),song.name(),volume));
        ClientPlayNetworking.send(new NbsNetworking.PlayRequest(pos,song.hash(),song.name(),volume));
    }
    public static void requestStop(BlockPos pos){ClientPlayNetworking.send(new NbsNetworking.StopRequest(pos)); ActiveSounds.stop(pos);}

    private static void handleUploadRequest(NbsNetworking.UploadRequest p) {
        SongLibrary.Song song=SongLibrary.find(p.hash());
        if(song==null)return;
        try {
            byte[] data=SongLibrary.read(song);
            if(data.length > 16*1024*1024)return;
            int chunk=48_000, total=(data.length+chunk-1)/chunk;
            ClientPlayNetworking.send(new NbsNetworking.UploadStart(song.hash(),song.name(),data.length));
            for(int i=0;i<total;i++){int a=i*chunk,b=Math.min(data.length,a+chunk);ClientPlayNetworking.send(new NbsNetworking.UploadChunk(song.hash(),i,total,Arrays.copyOfRange(data,a,b)));}
        } catch(Exception ignored){}
    }
    private static void handleUploadAccepted(NbsNetworking.UploadAccepted p){PendingPlay pending=PENDING.get(p.hash()); if(pending!=null) ClientPlayNetworking.send(new NbsNetworking.PlayRequest(pending.pos,pending.hash(),pending.name,pending.volume));}
    private static void handlePlayStart(NbsNetworking.PlayStart p) {
        try {
            byte[] data;
            if(SongLibrary.hasCached(p.hash())) data=SongLibrary.readCached(p.hash());
            else {
                SongLibrary.Song local=SongLibrary.find(p.hash());
                if(local!=null)data=SongLibrary.read(local);
                else { PENDING.put(p.hash(),new PendingPlay(p.pos(),p.hash(),p.name(),p.volume())); ClientPlayNetworking.send(new NbsNetworking.DownloadRequest(p.hash())); return; }
            }
            PENDING.remove(p.hash()); ActiveSounds.setName(p.pos(),p.name()); ActiveSounds.play(p.pos(),data,p.volume());
        } catch(Exception ignored){}
    }
    private static void handleDownloadChunk(NbsNetworking.DownloadChunk p){
        DownloadAssembly a=DOWNLOADS.computeIfAbsent(p.hash(),h->new DownloadAssembly(p.total()));
        if(p.index()<0||p.index()>=p.total()||a.total!=p.total())return;
        a.parts.put(p.index(),p.data());
        if(a.parts.size()==a.total){
            int size=a.parts.values().stream().mapToInt(x->x.length).sum();byte[] data=new byte[size];int at=0;
            for(int i=0;i<a.total;i++){byte[] c=a.parts.get(i);if(c==null)return;System.arraycopy(c,0,data,at,c.length);at+=c.length;}
            SongLibrary.writeCached(p.hash(),data);DOWNLOADS.remove(p.hash());PendingPlay pending=PENDING.remove(p.hash());
            if(pending!=null){ActiveSounds.setName(pending.pos,pending.name);ActiveSounds.play(pending.pos,data,pending.volume);}
        }
    }

    private record PendingPlay(BlockPos pos,String hash,String name,float volume){}
    private static final class DownloadAssembly {final int total;final Map<Integer,byte[]> parts=new HashMap<>();DownloadAssembly(int t){total=t;}}
}

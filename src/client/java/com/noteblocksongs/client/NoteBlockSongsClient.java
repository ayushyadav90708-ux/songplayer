package com.noteblocksongs.client;

import com.noteblocksongs.client.audio.PositionalSongPlayer;
import com.noteblocksongs.client.gui.SongsScreen;
import com.noteblocksongs.client.storage.SongLibrary;
import com.noteblocksongs.network.SongNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public final class NoteBlockSongsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SongLibrary.init();
        PositionalSongPlayer.init();
        SongNetworking.registerClient();

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.NOTE_BLOCK)
                    && hand == net.minecraft.util.Hand.MAIN_HAND) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.setScreen(new SongsScreen(hit.getBlockPos()));
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PositionalSongPlayer.shutdown());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                PositionalSongPlayer.tick(client);
            }
        });
    }
}

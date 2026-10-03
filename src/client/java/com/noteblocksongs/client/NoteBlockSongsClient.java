package com.noteblocksongs.client;

import com.noteblocksongs.client.audio.PositionalSongPlayer;
import com.noteblocksongs.client.gui.SongsScreen;
import com.noteblocksongs.client.storage.SongLibrary;
import com.noteblocksongs.network.SongNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;

public final class NoteBlockSongsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        SongLibrary.init();
        SongNetworking.registerClient();
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand == InteractionHand.MAIN_HAND && world.getBlockState(hit.getBlockPos()).is(Blocks.NOTE_BLOCK)) {
                Minecraft.getInstance().setScreen(new SongsScreen(hit.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(PositionalSongPlayer::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> PositionalSongPlayer.shutdown());
    }
}

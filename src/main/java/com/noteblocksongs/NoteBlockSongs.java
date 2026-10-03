package com.noteblocksongs;

import com.noteblocksongs.network.SongNetworking;
import com.noteblocksongs.server.SongServerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.level.ServerPlayer;

public final class NoteBlockSongs implements ModInitializer {
    public static final String MOD_ID = "noteblocksongs";

    @Override
    public void onInitialize() {
        SongNetworking.registerCommon();
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && state.isOf(Blocks.NOTE_BLOCK) && player instanceof ServerPlayer serverPlayer) {
                SongServerState.stopAt(serverPlayer.server, serverPlayer.serverLevel(), pos);
            }
        });
    }
}

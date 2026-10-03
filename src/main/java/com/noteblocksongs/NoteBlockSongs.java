package com.noteblocksongs;

import com.noteblocksongs.network.SongNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.util.ActionResult;

public final class NoteBlockSongs implements ModInitializer {
    public static final String MOD_ID = "noteblocksongs";

    @Override
    public void onInitialize() {
        SongNetworking.registerCommon();

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.NOTE_BLOCK)) {
                // GUI opening is client-side; the server accepts the interaction normally.
                return ActionResult.PASS;
            }
            return ActionResult.PASS;
        });
    }
}

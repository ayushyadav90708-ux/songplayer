package com.example.noteblocksongs;

import com.example.noteblocksongs.config.NbsConfig;
import com.example.noteblocksongs.network.NbsNetworking;
import com.example.noteblocksongs.server.NbsServerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

public final class NoteBlockSongs implements ModInitializer {
    public static final String MOD_ID = "note_block_songs";
    public static final Identifier MP3_SOUND_ID = Identifier.of(MOD_ID, "mp3_source");

    @Override
    public void onInitialize() {
        NbsConfig.load();
        Registry.register(Registries.SOUND_EVENT, MP3_SOUND_ID, SoundEvent.of(MP3_SOUND_ID));
        NbsNetworking.registerCommon();
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.getBlockState(hit.getBlockPos()).isOf(Blocks.NOTE_BLOCK)) return ActionResult.PASS;
            // Server consumes the vanilla Note Block action; the client entrypoint opens the GUI.
            return world.isClient ? ActionResult.PASS : ActionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(NbsServerState::tick);
    }
}

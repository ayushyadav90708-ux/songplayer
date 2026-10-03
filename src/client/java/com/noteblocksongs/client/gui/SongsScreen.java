package com.noteblocksongs.client.gui;

import com.noteblocksongs.client.audio.PositionalSongPlayer;
import com.noteblocksongs.client.storage.SongLibrary;
import com.noteblocksongs.network.SongNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SongsScreen extends Screen {
    private final BlockPos noteBlock;
    private final List<Path> songs = new ArrayList<>();
    private EditBox search;
    private int selected = -1;
    private int scroll = 0;
    private List<Path> filtered = List.of();
    private boolean draggingSlider = false;

    public SongsScreen(BlockPos noteBlock) { super(Component.literal("Songs")); this.noteBlock = noteBlock; }

    @Override protected void init() {
        reloadSongs();
        search = new EditBox(font, width / 2 - 145, 28, 290, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search songs..."));
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Play"), b -> playSelected()).bounds(width / 2 - 130, height - 46, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Stop"), b -> SongNetworking.requestStop(noteBlock)).bounds(width / 2 - 65, height - 46, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> reloadSongs()).bounds(width / 2, height - 46, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(width / 2 + 75, height - 46, 60, 20).build());
    }

    private void reloadSongs() { songs.clear(); songs.addAll(SongLibrary.scan()); updateFiltered(); }
    private void updateFiltered() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        filtered = songs.stream().filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(q)).toList();
        int max = Math.max(0, filtered.size() - 9); scroll = Math.min(scroll, max);
        if (selected >= filtered.size()) selected = -1;
    }
    private void playSelected() {
        if (selected < 0 || selected >= filtered.size()) return;
        try {
            Path p = filtered.get(selected);
            SongNetworking.requestPlay(noteBlock, p.getFileName().toString(), SongLibrary.read(p));
        } catch (Exception e) { System.err.println("[NoteBlockSongs] Play failed: " + e); }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        updateFiltered();
        int left = width / 2 - 155, right = width / 2 + 155;
        g.fill(left, 55, right, height - 58, 0xCC101010);
        g.drawCenteredString(font, "NOTE BLOCK SONGS", width / 2, 10, 0xFFFFFF);
        g.drawString(font, "Volume", left + 8, height - 72, 0xFFFFFF);
        g.fill(left + 58, height - 67, right - 8, height - 61, 0xFF555555);
        int knob = left + 58 + (int)((right - left - 66) * PositionalSongPlayer.volume());
        g.fill(knob - 3, height - 71, knob + 3, height - 57, 0xFFFFFFFF);
        if (filtered.isEmpty()) g.drawCenteredString(font, "No MP3 songs found in .minecraft/Songs", width / 2, 78, 0xAAAAAA);
        int visible = Math.min(9, filtered.size() - scroll);
        for (int row = 0; row < visible; row++) {
            int i = row + scroll, y = 62 + row * 25;
            boolean active = i == selected;
            g.fill(left + 8, y - 2, right - 8, y + 20, active ? 0xFF3A3A3A : 0xFF202020);
            g.drawString(font, "♪ " + filtered.get(i).getFileName(), left + 14, y + 4, active ? 0xFFFF55 : 0xFFFFFF);
        }
        if (filtered.size() > 9) {
            int trackTop = 62, trackBottom = 62 + 8 * 25;
            g.fill(right - 5, trackTop, right - 2, trackBottom, 0xFF555555);
            int max = filtered.size() - 9;
            int thumbY = trackTop + (int)((trackBottom - trackTop - 18) * (scroll / (double) max));
            g.fill(right - 7, thumbY, right, thumbY + 18, 0xFFFFFFFF);
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = width / 2 - 155, right = width / 2 + 155;
        if (button == 0 && mouseY >= 60 && mouseY < 62 + 9 * 25 && mouseX >= left && mouseX <= right - 10) {
            int row = (int)((mouseY - 60) / 25);
            int i = row + scroll;
            if (i >= 0 && i < filtered.size()) { selected = i; return true; }
        }
        if (button == 0 && mouseY >= height - 75 && mouseY <= height - 53 && mouseX >= left + 50 && mouseX <= right - 5) {
            draggingSlider = true; setVolume(mouseX, left); return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setVolume(double mouseX, int left) {
        double fraction = (mouseX - (left + 58)) / (double)(width - 2 * (width / 2 - 155) - 66);
        PositionalSongPlayer.setVolume((float)Math.max(0, Math.min(1, fraction)));
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingSlider && button == 0) { setVolume(mouseX, width / 2 - 155); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { if (button == 0) draggingSlider = false; return super.mouseReleased(mouseX, mouseY, button); }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, filtered.size() - 9);
        scroll = Math.max(0, Math.min(max, scroll - (int)Math.signum(verticalAmount)));
        return true;
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(null); }
}

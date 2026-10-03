package com.noteblocksongs.client.gui;

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

    public SongsScreen(BlockPos noteBlock) {
        super(Component.literal("Songs"));
        this.noteBlock = noteBlock;
    }

    @Override
    protected void init() {
        songs.clear();
        songs.addAll(SongLibrary.scan());

        search = new EditBox(this.font, this.width / 2 - 120, 32, 240, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search songs..."));
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("▶ Play"), b -> {
            if (selected >= 0 && selected < songs.size()) {
                try {
                    Path path = songs.get(selected);
                    SongNetworking.requestPlay(noteBlock, path.getFileName().toString(), SongLibrary.read(path));
                } catch (Exception ignored) {}
            }
        }).bounds(this.width / 2 - 125, this.height - 48, 78, 20).build());

        addRenderableWidget(Button.builder(Component.literal("■ Stop"), b ->
                SongNetworking.requestStop(noteBlock)
        ).bounds(this.width / 2 - 39, this.height - 48, 78, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            songs.clear();
            songs.addAll(SongLibrary.scan());
        }).bounds(this.width / 2 + 47, this.height - 48, 78, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, "Songs", this.width / 2, 12, 0xFFFFFF);

        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        List<Path> filtered = songs.stream()
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        int y = 62;
        for (int i = scroll; i < Math.min(filtered.size(), scroll + 10); i++) {
            Path p = filtered.get(i);
            boolean active = (i - scroll) == selected;
            int color = active ? 0xFFFF55 : 0xFFFFFF;
            g.drawString(this.font, "♪ " + p.getFileName(), this.width / 2 - 130, y, color);
            y += 22;
        }

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
            List<Path> filtered = songs.stream()
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(query))
                    .toList();

            int index = (int)((mouseY - 58) / 22) + scroll;
            if (index >= 0 && index < filtered.size()) {
                Path selectedPath = filtered.get(index);
                selected = songs.indexOf(selectedPath);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

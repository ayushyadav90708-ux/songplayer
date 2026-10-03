# Note Block Songs

Fabric mod for Minecraft Java Edition 1.21.11.

## What it does

- Creates `.minecraft/Songs/` automatically.
- Detects `.mp3` files.
- Right-clicking a Note Block opens the Songs screen.
- The selected MP3 is decoded locally with JLayer.
- The player sends the selected MP3 bytes to the logical server.
- The server validates the filename/size and broadcasts the audio to nearby modded clients.
- Receiving clients cache the MP3 bytes and play the song from the Note Block position.
- Playback has a configurable distance and volume.

## Important multiplayer behavior

Every listener must have the mod installed. They do **not** need to already have the MP3 in their own Songs folder: the server sends the audio bytes to nearby modded clients and those clients cache/play it.

This implementation intentionally limits transfers to 20 MiB and rejects unsupported/oversized files.

## Build

Use Java 21.

```text
./gradlew build
```

The remapped JAR is produced in:

`build/libs/note-block-songs-1.0.0.jar`

Put the JAR in the Fabric `mods` folder.

## Songs folder

The mod creates:

`.minecraft/Songs/`

Put MP3 files there.

## Version support

This repository is a real 1.21.11 build. Minecraft 1.21 through 1.21.10 require their own version-specific Fabric/Loom/mappings builds because Minecraft/Fabric APIs changed during the 1.21 release line.

Do not put this 1.21.11 JAR into another Minecraft version.

The intended porting targets are:

1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11.

## Security

The server checks:

- file extension
- canonical path stays inside Songs
- maximum file size
- playback range
- number of active songs per player
- packet/request structure

For a public server, consider adding an operator-only permission for song playback. The current build uses an application-level 20 MiB limit and Fabric large-payload registration; production deployments may want a smaller configurable limit.

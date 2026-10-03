# Note Block Songs 1.0.0

Fabric mod for Minecraft Java Edition **1.21.11**. It adds an MP3 library to Note Blocks and synchronizes the selected audio through the modded Minecraft server.

## Important version note
Minecraft 1.21 through 1.21.11 are not one binary-compatible mod target. Fabric confirms that 1.21.11 is the last obfuscated release and that 26.1+ is a different, unobfuscated toolchain. This project therefore targets **1.21.11 explicitly** rather than falsely claiming that one JAR works across every 1.21.x release. Older 1.21.x ports require their own version-specific mappings/API build. See `VERSIONS.md`.

## Features
- Creates `.minecraft/Songs/` automatically.
- Scans MP3 files when the Songs screen opens or Refresh is pressed.
- Right-click a Note Block to open the **Songs** GUI.
- Search, scrollable song list, Play, Stop, Refresh and volume slider.
- The initiating client uploads the selected MP3 to the server only when the server does not already have that content hash.
- Server stores a cache keyed by SHA-256 and distributes it to nearby modded clients.
- Clients cache received MP3s locally and do not repeatedly transfer the same file.
- Positional playback at the Note Block with linear attenuation and configurable range.
- Server-side size, upload, active-source, range and chunk limits.
- Playback is stopped when the Note Block is broken or disappears.

## Build
Requires JDK 21. The project uses Fabric Loom 1.14, Fabric Loader 0.18.1 and Fabric API 0.141.3+1.21.11.

If Gradle is installed:

```text
gradle build
```

If using the supplied bootstrap scripts, run `gradlew.bat build` on Windows or `./gradlew build` on Linux/macOS. The bootstrap downloads the Gradle distribution when necessary. The official Gradle wrapper JAR is intentionally not vendored by this generated package; after the first successful Gradle installation you can run `gradle wrapper --gradle-version 8.10.2` to generate the conventional wrapper files.

The production JAR is created under `build/libs/` and is the JAR without the `-dev` classifier.

## Install
1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API for 1.21.11.
3. Put the built `note-block-songs-1.0.0.jar` into the client's `mods` folder.
4. For multiplayer, the **server must also have the same mod installed**. This is required because the server validates uploads, stores the cache and relays synchronized playback.
5. Launch Minecraft. The mod creates `.minecraft/Songs/` automatically.
6. Put MP3 files in that folder.
7. Right-click a Note Block and choose a song.

## Limits / configuration
Edit `config/note_block_songs.json` after first launch:
- `max_file_size_mb`: maximum MP3 upload size, default 16 MB.
- `max_duration_seconds`: documented policy limit; MP3 uploads are rejected if their estimated duration is clearly above the limit.
- `max_transfer_bytes_per_second`: per-player upload budget.
- `max_active_sources`: maximum simultaneous Note Block sources per server.
- `max_playback_range`: maximum allowed source range.
- `cache_limit_mb`: server-side cache size.
- `volume`: local master volume multiplier.

The client and server use the same JSON file format, but server-side values are authoritative for network safety.

## Protocol
`PlayRequest` -> server validates the block and requested hash. If the server cache is missing, the server asks the initiating client for an upload. The server stores the bytes by SHA-256, then broadcasts a `PlayStart`. Clients missing the cache request it with `DownloadRequest`; the server sends bounded chunks. A `Stop` packet is sent when playback is stopped or the source block is gone.

This avoids making every client independently search its own Songs folder and allows players without the original MP3 to hear the song.

## Legal / privacy
Only transfer audio files that you have permission to distribute to other players on the server. The mod intentionally sends the initiating player's selected MP3 to the server so that other modded clients can hear it.

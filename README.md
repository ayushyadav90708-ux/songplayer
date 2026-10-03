# Note Block Songs — Version 2 (Minecraft 1.21.11 / Fabric)

## What Version 2 fixes
- Right-click a Note Block to open the Songs GUI.
- Automatically creates `.minecraft/Songs/`.
- MP3 search, scrolling, selection, Play, Stop, Refresh and Close controls.
- Volume slider in the GUI.
- Client sends the selected MP3 to the server; the server validates it, caches it by SHA-256 and broadcasts it to nearby modded clients.
- Players nearby can hear a song even if they do not have the MP3 locally.
- Positional attenuation is handled with OpenAL and the source follows the Note Block while the Minecraft listener follows the player/camera.
- Active playback is stopped when Stop is pressed or the Note Block is broken.
- 20 MiB per-song limit and server-side filename/position/hash validation.

## Build
Requires Java 21 and internet access for Gradle dependencies.

Windows:
`gradlew.bat build`

Linux/macOS:
`./gradlew build`

The built JAR is in `build/libs/`.

## Install
Install Fabric Loader + Fabric API for Minecraft 1.21.11, then put the built mod JAR in `.minecraft/mods/` on the client and on the server if using a dedicated server.

Create `.minecraft/Songs/` automatically by launching the game once with the mod. Drop MP3 files into that folder, right-click a Note Block, select a song and press Play.

## Multiplayer
The server is the relay/cache authority. A client uploads the MP3 once when Play is pressed. The server caches the bytes using the SHA-256 hash and sends the song to compatible clients within 40 blocks of the Note Block. Recipients do not need the MP3 in their own Songs folder.

## Important
This release targets Minecraft 1.21.11 specifically. Minecraft 1.21–1.21.10 should use their own version-specific builds because Minecraft/Fabric mappings and APIs can differ.

## Build verification note
The source and project structure were checked during creation, and the Fabric 1.21.11 API/networking model was cross-checked against Fabric's 1.21.11 API documentation. This environment does not have a Gradle installation and cannot download the Gradle distribution, so I have **not** falsely labeled this ZIP as a successfully compiled JAR. Run the build on a machine with Gradle 8.14.3/network access; fix any mapping/API error reported by your exact local Fabric toolchain before deployment.

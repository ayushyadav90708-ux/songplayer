# Minecraft version matrix

| Minecraft | Status |
|---|---|
| 1.21 | Separate port required |
| 1.21.1 | Separate port required |
| 1.21.2 | Separate port required |
| 1.21.3 | Separate port required |
| 1.21.4 | Separate port required |
| 1.21.5 | Separate port required |
| 1.21.6 | Separate port required |
| 1.21.7 | Separate port required |
| 1.21.8 | Separate port required |
| 1.21.9 | Separate port required |
| 1.21.10 | Separate port required |
| **1.21.11** | **Implemented target** |

Fabric's documentation notes that 1.21.11 is the last obfuscated Minecraft release and recommends the 1.21.11 Loom/toolchain separately. The networking API also gained the `registerLarge` path used for large payloads. Because mappings and Minecraft APIs change across these releases, this project deliberately does not ship a misleading single multi-version JAR. The source architecture is isolated so the network/audio protocol can be reused when creating each port.

For an older release, create a version-specific branch/project using that release's recommended Fabric Loader, Fabric API, Loom and mappings, then port the small Minecraft-facing classes (screen widgets, block interaction, networking buffer type and sound classes). The MP3 decoder, cache, hashing and protocol concepts are version-independent.

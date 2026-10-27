# SnowballFight

This is BeyoraCraft's development fork of [SnowballFight by xGinko](https://github.com/xGinko/SnowballFight).
The original author credits and plugin identity are retained while the fork is being prepared.

Build with a JDK 21 toolchain and the included Gradle wrapper: `./gradlew clean build`
(Windows: `.\gradlew.bat clean build`). The build produces one bundled plugin JAR in `build/libs/`
and targets Java 21 directly; Java 8 bytecode conversion has been removed.

The server target for this fork is Leaf 1.21.11 (Paper API). Minecraft 26.3 support and world filtering
are still planned. A successful build does not confirm in-game compatibility, and legacy Spigot
compatibility is no longer a development target.

[<img src="https://img.youtube.com/vi/pYHbJ0BOXks/hqdefault.jpg" width="560" height="315"/>](https://www.youtube.com/embed/pYHbJ0BOXks)

## Epic snowball battles. Lots of config freedom.
#### Original upstream description

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Z8Z3RVMAJ)

### Description
SnowballFight aims to be highly configurable, adding various features to snowballs in-game:

- snow spread effect when a snowball hits a player, entity or a block
- explosions when a snowball hits a player, entity or a block
- firework effects when a snowball hits a player, entity or a block
- trailing effects when a snowball is thrown or launched
- knockback when a snowball hits a player or an entity
- lightning strikes when a snowball hits a player, entity or a block
- levitation when a snowball hits a player or an entity
- slowness when a snowball hits a player or an entity
- snowball launch cooldown for players, entities and blocks
- ability to make snowballs never consume for players
- drop configurable items from a player's armor slot when hit by a snowball
- Configurable colors, so you can use it with your servers color theme if you want

All features offer lots of customizable options so you can configure each feature according to your preferences!

### Commands:
- /snowballs reload - Reloads the plugin configuration
- /snowballs version - Shows the plugin version
- /snowballs disable - Disable the plugin for testing or better serverutil/plugman reload

### Permissions:
- snowballfight.cmd.reload - Permission to run /snowballs reload
- snowballfight.cmd.version - Permission to run /snowballs version
- snowballfight.cmd.disable - Permission to run /snowballs disable

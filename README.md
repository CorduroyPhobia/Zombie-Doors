# Zombie Doors

![Zombie Doors](docs/zombie-doors.gif)

Zombies pick up wooden doors and use them to block attacks, hit players, and
shelter from sunlight. Arrows stick in the door, blocked hits wear it down, and
axes disable blocking for six seconds.

Adult zombies, husks, and zombie villagers can carry doors. They can also spawn
with one, with the wood type chosen by biome. Door breaking follows vanilla
rules unless you enable the override in the config.

## Install

[Downloads](https://github.com/CorduroyPhobia/Zombie-Doors/releases) are available
for Minecraft 26.1, 26.1.1, 26.1.2, 26.2, and 26.3. Use the JAR for your version;
the `-sources.jar` is for reading the code.

Requires Fabric Loader, Fabric API, and Java 25. Install the mod and Fabric API
on the server and each client. Mod Menu and Cloth Config are optional, for the
configuration screen.

## Configuration

Edit `config/zombiedoors.json`, or use Mod Menu in singleplayer. On a dedicated
server, run `/reload` after editing the file. See [Configuration](docs/CONFIGURATION.md)
for defaults and biome overrides.

## Build

This branch targets Minecraft 26.1.2.
Use Java 25 and run:

```powershell
.\gradlew.bat build
```

The JAR is written to `build/libs/`. To launch Minecraft with the mod in development,
run `.\gradlew.bat runClient`.

## License

Created by Corduroy Phobia. All rights reserved; see [LICENSE](LICENSE).

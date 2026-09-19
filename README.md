# Zombie Doors

![Zombie Doors](docs/zombie-doors.gif)

Zombies pick up wooden doors and use them to block attacks, hit players, and
shelter from sunlight. Arrows and tridents stick in the door, blocked hits wear it down, and
axes disable blocking for six seconds.

Adult zombies, husks, and zombie villagers can carry doors. Zombified piglins
carry crimson and warped doors. Babies and drowned cannot carry them.

Carriers can spawn with a door, with the wood type chosen by biome. Door breaking
follows vanilla rules unless you enable the override in the config. Have your
attack blocked to earn **The Zombies Are Coming** advancement.

![Zombies, a husk, a zombie villager, and zombified piglins with doors](docs/images/door-guards.png)

Doors move with the zombie as it takes cover, swings, and recovers from a hit.

![Door blocking, overhead cover, and attacks](docs/images/door-combat.gif)

Blocked hits leave cracks in the door until it breaks.

![An intact door, a damaged door, and the moment it breaks](docs/images/door-wear.png)

![A carried door cracking and breaking under repeated attacks](docs/images/shield-breaking.gif)

Zombies can pick up the wooden doors they break down.

![A zombie breaking a placed door and taking it as a shield](docs/images/door-breaking.gif)

![A trident embedded in a door](docs/images/trident-block.png)

Embedded tridents drop when the door breaks or the carrier loses it. Loyalty
tridents return to their owner.

![Crimson and warped doors carried by zombified piglins](docs/images/nether-guards.png)

![A zombie sheltering under its door](docs/images/taking-cover.png)

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

`main` targets Minecraft 26.1.2. Other versions have a `minecraft/<version>` branch.
Use Java 25 and run:

```powershell
.\gradlew.bat build
```

The JAR is written to `build/libs/`. To launch Minecraft with the mod in development,
run `.\gradlew.bat runClient`.

## License

Created by Corduroy Phobia. All rights reserved; see [LICENSE](LICENSE).

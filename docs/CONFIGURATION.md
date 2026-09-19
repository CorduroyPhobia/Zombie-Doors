# Configuration

Edit `config/zombiedoors.json` in the Minecraft instance or server directory.
The development client uses `run/config/zombiedoors.json`.

On a dedicated server, run `/reload` after editing the file. Clients receive the
server's settings when they join and after a reload. Mod Menu shows those values
as read-only. Singleplayer settings are editable with Mod Menu and Cloth Config.

| Setting | Default | Range |
| --- | --- | --- |
| `enableReliableZombieDoorBreaking` | `false` | Boolean |
| `enableZombieDoorShields` | `true` | Boolean |
| `zombieDoorShieldSpawnChance` | `0.05` | 0–0.5; doubled on Hard |
| `zombieDoorShieldDurability` | `48` | 1–512 |
| `zombieDoorShieldAxeDisableSeconds` | `6` | 0–20 |
| `zombieDoorWhackCooldownSeconds` | `1.6` | 1–5 |
| `zombieDoorWhackReachBonus` | `0.75` | 0–2 blocks |
| `zombieDoorPoseTransitionSeconds` | `0.35` | 0.05–1 |
| `zombieDoorPoseCooldownSeconds` | `0.5` | 0–2 |
| `biomeDoorOverrides` | `{}` | Up to 128 mappings |

Door breaking uses vanilla eligibility, Hard difficulty, and Mob Griefing by
default. Enabling `enableReliableZombieDoorBreaking` adds a breaking goal for all
zombies on Easy, Normal, and Hard. It still respects Mob Griefing.

## Biome doors

These choices apply when a mob spawns with a door. Existing carriers keep their
door when moving between biomes. Picked-up and broken doors keep their wood.

| Biome | Door |
| --- | --- |
| Birch forests, including old-growth birch forests | Birch |
| Forest | Oak; 5% birch |
| Plains, swamp, dripstone caves | Oak |
| Dark forest and badlands | Dark oak |
| Pale garden | Pale oak |
| Taigas and cold biomes, including cold oceans | Spruce |
| Savannas and stony peaks | Acacia |
| Jungles, sparse jungles, desert | Jungle |
| Bamboo jungle | Bamboo |
| Cherry grove | Cherry |
| Mangrove swamp | Mangrove |
| Crimson / warped forest | Crimson / warped |
| Other biomes | Oak |

Modded biomes also recognize Fabric's conventional biome and primary-wood tags.
Biomes with a base temperature of 0.2 or lower use spruce when no wood mapping
is available. Other unrecognized biomes use oak.

Adult zombified piglins carry crimson and warped doors by default. They use their
forest's door in the Nether, or choose either at random elsewhere. Babies and
drowned cannot acquire or use doors. Zombies drop their door before converting
to drowned.

Add `biomeDoorOverrides` alongside the other settings:

```json
"biomeDoorOverrides": {
  "minecraft:desert": "minecraft:acacia_door",
  "#minecraft:is_forest": "minecraft:birch_door"
}
```

An exact biome ID takes priority over a tag. Matching tags are checked in
alphabetical order. Missing items and unsupported doors fall back to the next
matching tag or the built-in choice.

## Other mods

Doors must use Minecraft's `DoorBlock` class, including subclasses, and have the
`minecraft:wooden_doors` item or block tag. Their own models, textures, and sounds
are used. Doors with custom renderers or a different shape may need a separate
integration; the shield pose assumes a normal two-block door.

For biomes outside the `minecraft` namespace, automatic matching first checks
`c:primary_wood_type/<wood>` tags. A `c:primary_wood_type/maple` biome uses
`maple_door` from the biome's mod when available, or a matching door from another
installed mod. If several woods match, each has an equal chance.

Without a matching wood tag, standard tree features are checked for trunk blocks
ending in `_log`, `_wood`, `_stem`, or `_hyphae`. For example,
`examplemod:maple_log` matches `examplemod:maple_door`. Custom world-generation
systems that do not expose standard tree features can use an explicit mapping:

```json
"biomeDoorOverrides": {
  "examplemod:maple_forest": "examplemod:maple_door",
  "#examplemod:redwood_biomes": "examplemod:redwood_door"
}
```

Config mappings take priority. Missing mods and unsupported doors are skipped,
then biome-family tags and the defaults above provide a fallback. Automatic wood
matching does not change the built-in choices for vanilla biomes.

Data packs can add these mappings without editing the config:

- `data/zombiedoors/tags/worldgen/biome/doors/examplemod/maple_door.json`
  lists the biomes that should use `examplemod:maple_door`. These mappings take
  priority over automatic wood matching; multiple matching doors form an equal
  pool. Config overrides still win.
- `data/zombiedoors/tags/item/door_shields.json` allows additional `DoorBlock`
  items that lack wooden-door tags.
- `data/zombiedoors/tags/item/piglin_doors.json` allows additional supported doors
  for zombified piglins. By default this contains crimson and warped doors.

For example, the biome tag file above can contain:

```json
{
  "replace": false,
  "values": ["examplemod:maple_forest", "examplemod:maple_hills"]
}
```

The item tag files use the same format with item IDs in `values`. Tags can be
changed with `/reload`. Vanilla door-breaking eligibility, difficulty, and Mob
Griefing rules still apply unless the breaking override is enabled.

## Combat

A door held in front blocks melee attacks, including sword sweeps, using vanilla
shield damage-type and angle rules. Each blocked hit costs the incoming damage
rounded up, with a minimum of one durability point. Rear hits, hits during a
door swing, and hits while the door is overhead reach the zombie.

The first blocked axe hit damages the door, then disables blocking and sunlight
protection. Follow-up attacks reach the zombie. The disable check uses vanilla
weapon behavior, including compatible modded weapons. Set the disable duration
to zero to turn it off. The zombie can still attack with a disabled door.

Rejected hits and indirect damage from an axe-holding attacker do not disable
the door. Piercing arrows bypass it. Other blocked projectiles wear it down;
the hit that breaks a door is still blocked. Up to 12 arrows and tridents can
remain embedded in each door. Recoverable tridents drop when the door is lost
or broken, keeping their durability and enchantments. Loyalty tridents return
normally instead of remaining in the door.

A blocked player attack earns **The Zombies Are Coming** advancement. Both
melee attacks and player-fired projectiles count.

Blocked arrows and melee hits share a short recoil. A disabled door stays in the
braced position. Cracks show remaining durability, and existing doors retain
their original maximum durability after config changes or saving.

Carrying a door reduces movement speed by 15%, or 8% while it is overhead.
Arrow and trident impacts apply a 25% slowdown for 30 ticks. Sun-sensitive
carriers seek shade unless a target is in melee range; husks and zombified piglins
keep their sunlight immunity and helmets still work. Door attacks have a windup sound, a
contact-timed hit, and the configured cooldown and reach bonus.

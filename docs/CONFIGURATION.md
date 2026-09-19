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
| Dark forest | Dark oak |
| Pale garden | Pale oak |
| Taigas, groves, windswept hills/forests, snowy plains/slopes/beaches, ice spikes, frozen/jagged peaks, frozen rivers | Spruce |
| Savannas | Acacia |
| Jungles and sparse jungles | Jungle |
| Bamboo jungle | Bamboo |
| Cherry grove | Cherry |
| Mangrove swamp | Mangrove |
| Crimson / warped forest | Crimson / warped |
| Other biomes | Oak |

Modded biomes with vanilla jungle, savanna, taiga, or hill tags follow those
families. Others use oak unless configured below.

Add `biomeDoorOverrides` alongside the other settings:

```json
"biomeDoorOverrides": {
  "minecraft:desert": "minecraft:acacia_door",
  "#minecraft:is_forest": "minecraft:birch_door"
}
```

An exact biome ID takes priority over a tag. Matching tags are checked in
alphabetical order. Missing items and unsupported doors fall back to the next
matching tag or the built-in choice. Modded doors must have the wooden-door item
tag and use a door block.

IDs are limited to 256 characters after normalization. Invalid JSON or malformed
IDs leave the last valid settings active and log an error.

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
the hit that breaks a door is still blocked. Embedded arrows remain until the
door is lost or broken, up to 12 arrows per door.

Blocked arrows and melee hits share a short recoil. A disabled door stays in the
braced position. Cracks show remaining durability, and existing doors retain
their original maximum durability after config changes or saving.

Carrying a door reduces movement speed by 15%, or 8% while it is overhead.
Arrow and trident impacts apply a 25% slowdown for 30 ticks. Sun-sensitive
carriers seek shade unless a target is in melee range; husks keep their sunlight
immunity and helmets still work. Door attacks have a windup sound, a
contact-timed hit, and the configured cooldown and reach bonus.

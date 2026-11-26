# Effirromplus

Adds two enchantment levels above vanilla, as rare enchanted books:

- Unbreaking IV (vanilla stops at III)
- Efficiency VI (vanilla stops at V)

Works on a server and on a client. Install it on the server and players join with plain Minecraft, or install it in your own game to use it in singleplayer. The same jar does both.

## How to get them

| Source | Details |
|---|---|
| Librarians | Novice level. 30 to 64 emeralds (40 on average) plus a book. Not every librarian has them. |
| Bastion remnants, ancient cities, abandoned mineshafts | Every structure has at least one book in one of its chests. A second book is rare (15%) and a third is very rare (3%). Which chest holds it is decided when the structure is generated, so the order you open chests in does not matter. |
| End cities | 10% per chest. |
| Fishing | 0.3% per catch, instead of the normal catch. |
| Anvil | Two Efficiency V make Efficiency VI. Two Unbreaking III make Unbreaking IV. Works with books and tools. VI and IV are the new maximum. |

The enchanting table never gives these levels, and neither do loot tables or the random book a librarian sells. Nothing else about vanilla enchanting changes.

## Config

`config/effirromplus.json` is created on first start. Missing keys are added without touching your values.

| Key | Default |
|---|---|
| `villagers.enabled` | `true` |
| `villagers.minPrice`, `maxPrice`, `averagePrice` | `30`, `64`, `40` |
| `villagers.maxUses` | `12` |
| `structures.bastions`, `ancientCities`, `mineshafts` | `true` |
| `structures.secondBookChance`, `thirdBookChance` | `0.15`, `0.03` |
| `endCities.enabled`, `chancePerChest` | `true`, `0.10` |
| `fishing.enabled`, `chancePerCatch` | `true`, `0.003` |

## Versions

| Minecraft | Loaders | Java |
|---|---|---|
| 26.3 | Fabric, NeoForge, Forge | 25 |
| 26.1.2 | Fabric, NeoForge, Forge | 25 |
| 1.21.11 | Fabric, NeoForge, Forge | 21 |
| 1.18.2 | Fabric, Forge | 17 |
| 1.16.5 | Fabric, Forge | 8 |

Fabric needs Fabric API. Ancient cities do not exist in 1.18.2 and 1.16.5, so those versions only place books in bastions and mineshafts.

## Building

```
./gradlew build
./gradlew dist
```

`dist` collects the three jars for 26.3 in `build/dist`. Older versions live in `versions/<minecraft version>` and build with `./gradlew -p versions/<minecraft version> dist`.

## License

MIT

# CRPI-FakePlayer

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=flat-square)]()
[![Carpet](https://img.shields.io/badge/Carpet-1.4.194-4A90D9?style=flat-square)]()
[![Mod Loader](https://img.shields.io/badge/Loader-Fabric-8b6b9c?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)]()
[![Version](https://img.shields.io/badge/Version-0.1.0-orange?style=flat-square)]()

A server-side action framework for Carpet fake players (Fabric + Carpet extension). Drives fake players through a unified Action API that executes near-vanilla player behaviours — **pure server-side, no client installation, zero Mixins, zero extra threads**.

- Minecraft **1.21.11** / Fabric Loader 0.19.x / Carpet **1.4.194**
- Current version: **0.1.0** (V0.1–V0.4 complete)

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.x |
| Carpet Mod | 1.4.194 |
| Java | 21 |

## Installation

1. Put `crpi-fakeplayer-0.1.0.jar` into the server's `mods/` folder
2. **Nothing** needs to be installed on the client
3. Verify: `/crpi-fakeplayer list` should list 10 rules

## Features

All actions execute through vanilla player entry points (damage, mining formula, container logic, item use are all native):

| Action | Description |
|---|---|
| `ATTACK` | attack entities (`ServerPlayerEntity.attack`) |
| `DROP_ITEM` | drop items, main/off hand, single/entire stack |
| `CLOSE_GUI` | close open containers (full `onClosed` lifecycle) |
| `USE_ITEM` | use the held item (`interactItem`) |
| `DIG` | mine blocks (native `calcBlockBreakingDelta` + `finishMining`) |
| `USE` | right-click blocks: place, open chests, buttons, doors |
| `INTERACT_ENTITY` | villager trades, riding, feeding |
| `GUI_CLICK` | container slot clicks (`ScreenHandler.onSlotClick`) |
| `USE_RELEASE` | hold-and-release item use (bow draw 20 ticks = full power) |
| `CONTAINER_SCAN` | read-only container scan around the fake player |

## Commands

```
/crpi fp list | info | attack | drop | close | useitem | dig | use | interact | userelease | scancontainers | gui info|list|click|close
```

## Rules

`fakePlayerActions` / `fakePlayerCombat` / `fakePlayerItemUse` / `fakePlayerInteraction` / `fakePlayerMining` / `fakePlayerContainer` / `fakePlayerDebug` / `maxQueueLength` / `maxConcurrentActions` / `maxContainerScanRadius`

## Action API

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

FakePlayerActions.of(bot).attack(zombie).execute();
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
FakePlayerActions.of(bot).use(pos, Direction.UP).execute();
FakePlayerActions.of(bot).useRelease(Hand.MAIN_HAND, 20).execute();
FakePlayerActions.of(bot).clickSlot(0, 0, SlotActionType.PICKUP).execute();
List<ContainerScanResult> found = FakePlayerActions.of(bot).scanContainers(16);
```

## Documentation

Full feature documentation (Chinese): [docs/](docs/)

## Building

JDK 21 required:

```
gradlew.bat build
```

Output jar: `build/libs/`.

## License

MIT — see [LICENSE](LICENSE).

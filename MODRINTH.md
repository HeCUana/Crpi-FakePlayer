# CRPI-FakePlayer

**A server-side action framework for Carpet fake players.** Drive fake players through a unified Action API that executes near-vanilla player behaviours — mining, container management, combat, item use — all through Minecraft's own player logic.

## Features

| Action | Description |
|---|---|
| `ATTACK` | Attack entities (vanilla damage / knockback / enchantments / cooldown) |
| `DROP_ITEM` | Drop items — main/off hand, single or entire stack |
| `CLOSE_GUI` | Close open containers (full `onClosed` lifecycle) |
| `USE_ITEM` | Use the held item |
| `DIG` | Mine blocks (native mining formula: hardness, tools, enchantments, water penalty) |
| `USE` | Right-click blocks: place blocks, open chests, press buttons, toggle doors — mod blocks included |
| `INTERACT_ENTITY` | Interact with entities: villager trades, riding, feeding |
| `GUI_CLICK` | Container slot clicks (native `ScreenHandler.onSlotClick` — PICKUP / QUICK_MOVE / SWAP / CLONE / THROW / PICKUP_ALL) |
| `USE_RELEASE` | Hold-and-release item use: bow draw (20 ticks = full power), eating, shield blocking |
| `CONTAINER_SCAN` | Read-only scan of containers in a radius (no GUI opened, no chunks loaded, locked-container and distance aware) |

Plus a full framework for other mods: `Action` / `ActionDispatcher` / `ActionScheduler` (tick-driven, no extra threads) / per-player queues, and a fluent `FakePlayerActions` API.

## Why CRPI-FakePlayer

- **Pure server-side** — clients install nothing
- **Zero Mixins** — every behaviour goes through vanilla entry points (`ServerPlayerEntity`, `ServerPlayerInteractionManager`, `ScreenHandler`, item use lifecycle)
- **Native results** — damage, mining speed, container rules and item behaviour are exactly what a real player would produce
- **Tick-safe** — all actions run on the server thread via the Carpet `onTick` hook

## Requirements (server only)

- Minecraft **1.21.11**
- Fabric Loader **0.19.x**
- Carpet Mod **1.4.194**
- Java 21

## Quick start

```
/player bot1 spawn

/crpi fp dig bot1 100 64 200 down       # mine a block
/crpi fp use bot1 101 64 200 east       # open a chest
/crpi fp gui click bot1 0 0 pickup      # take from slot 0
/crpi fp gui close bot1                 # close the chest
/crpi fp userelease bot1 20             # draw a bow for 1s and fire
/crpi fp scancontainers bot1 16         # scan containers in a 16-block radius
```

All commands live under `/crpi fp` (OP level 2) and are toggled by 10 Carpet rules (`/crpi-fakeplayer <rule> <value>`).

## For developers

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

FakePlayerActions.of(bot).attack(zombie).execute();
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
FakePlayerActions.of(bot).useRelease(Hand.MAIN_HAND, 20).execute();
List<ContainerScanResult> found = FakePlayerActions.of(bot).scanContainers(16);
```

Full API docs: [docs/API.md](https://github.com/HeCUana/Crpi-FakePlayer/blob/main/docs/API.md)

## Documentation

- [README](https://github.com/HeCUana/Crpi-FakePlayer) (EN/ZH)
- [Feature documentation (ZH)](https://github.com/HeCUana/Crpi-FakePlayer/blob/main/docs/crpi-Fakeplayer%E5%8A%9F%E8%83%BD%E6%96%87%E6%A1%A3.md)
- [API documentation](https://github.com/HeCUana/Crpi-FakePlayer/blob/main/docs/API.md)

## License

MIT

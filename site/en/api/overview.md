# API Overview

CRPI-FakePlayer provides three API surfaces, all accessible through `FakePlayerHandle`.

## Architectural Rules

Before using the API, understand the two inviolable constraints:

1. **Zero Mixin / Zero New Threads** — All behaviors execute through native Minecraft entry points (`ServerPlayerEntity.attack`, `dropItem`, `closeHandledScreen`, `ServerPlayerInteractionManager.interactBlock`/`interactItem`/`finishMining`, `ScreenHandler.onSlotClick`, `onStoppedUsing`/`finishUsing`). Damage, enchantments, cooldowns, mining formulas, item logic all stay native.
2. **Tick-Driven** — All sustained behaviors step on the server main thread per tick. No new threads, no async callbacks.

## Tick Chain

```
Server tick → Carpet extension onTick (CRPIFakePlayerMod.onTick)
  → FakePlayerRegistry.tick    (cleanup disconnected bots)
  → ActionScheduler.tick       (stateful actions: DIG / USE_RELEASE)
  → ControlManager.tick        (MoveTask / LookTask continuous tasks)
  → NavigationRegistry.tick    (per-bot NavigationManager)
```

## Getting a Handle

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");
```

`FakePlayerHandle` is a thin wrapper over `ServerPlayerEntity`. The adapter is the ONLY class that references Carpet internals (`EntityPlayerMPFake`).

## Three API Surfaces

### Action API

```java
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
```

9 native behavior types, immediate or stateful. See [Action API](./actions.md).

### Control API

```java
bot.control().moveTo(new BlockPos(100, 64, 200), 1.0);
bot.control().lookAt(targetEntity);
bot.control().sneak(true);
```

Movement, look, state, inventory, riding, command execution. See [Control API](./control.md).

### Navigation API

```java
bot.navigation().gotoBlock(new BlockPos(200, 64, 300));
bot.navigation().follow(targetEntity, 3);
```

A* pathfinding + physics-native execution. See [Navigation API](./navigation.md).

## ActionPipeline

```java
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .timeout(100)
    .retry(3)
    .onSuccess(r -> bot.control().sendChatMessage("Done!"))
    .execute(bot);
```

Behavior orchestration combinator. See [ActionPipeline](./pipeline.md).

## Package Structure

```
com.crpi.fakeplayer
├── action/          # Behavior framework (ActionType, Action, ActionDispatcher)
│   ├── impl/        # 9 behavior implementations
│   └── executor/    # Behavior executors
├── api/             # Public API surface
│   └── pred/        # Predicates (HasItem)
├── command/         # /crpi fp command tree
├── config/          # Carpet rules
├── container/       # Container scanning
├── control/         # Control system (MoveTask, LookTask)
├── fakeplayer/      # Handle adapter
├── itemuse/         # Item use session
├── mining/          # Mining session
├── navigation/      # Pathfinding engine
│   ├── cost/        # Cost model
│   ├── executor/    # Path executor
│   ├── goal/        # Goal types
│   ├── movement/    # 8 movement types
│   ├── pathfinding/ # A* algorithm
│   └── world/       # World abstraction
└── scheduler/       # Action scheduler
```

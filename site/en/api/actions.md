# Action API

Action API is the core behavior system of CRPI-FakePlayer, providing 9 native Minecraft behavior types.

## Quick Start

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// Using facade methods
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
FakePlayerActions.of(bot).attack(entity).execute();
FakePlayerActions.of(bot).drop().execute();
```

## ActionType Enum

| ActionType | Description | Execution |
|---|---|---|
| `ATTACK` | Attack entity | Immediate |
| `DROP_ITEM` | Drop item | Immediate |
| `CLOSE_GUI` | Close container | Immediate |
| `USE_ITEM` | Use item | Immediate |
| `DIG` | Dig block | Stateful (tick-driven) |
| `USE` | Right-click interact | Immediate |
| `INTERACT_ENTITY` | Entity interact | Immediate |
| `GUI_CLICK` | Container slot click | Immediate |
| `USE_RELEASE` | Hold and release | Stateful (tick-driven) |

## ActionResult Enum

| Result | Description |
|---|---|
| `SUCCESS` | Executed successfully |
| `PASS` | Skipped (precondition not met) |
| `FAIL` | Execution failed |
| `RETRY` | Needs retry |
| `SKIP` | Skipped |
| `ABORT` | Aborted (timeout, etc.) |
| `INVALID_TARGET` | Target invalid |
| `OUT_OF_RANGE` | Out of range |
| `NO_PERMISSION` | No permission |
| `INVALID_STATE` | State invalid |
| `CONCURRENCY_LIMIT` | Concurrency limit |

## ActionState Lifecycle

```
CREATED → QUEUED → STARTED → RUNNING → SUCCESS
                                   → FAILED
                                   → CANCELLED
```

## Behavior Details

### ATTACK

Attack an entity, using native damage calculation (enchantments, knockback, cooldowns).

```java
FakePlayerActions.of(bot).attack(targetEntity).execute();
```

### DIG

Dig a block, using native mining formula (block hardness, tool efficiency, enchantments, water penalty).

```java
FakePlayerActions.of(bot).dig(new BlockPos(100, 64, 200), Direction.UP).execute();
```

::: tip
DIG is a stateful behavior, driven per-tick by `ActionScheduler` until the block is broken.
:::

### DROP_ITEM

```java
// Drop main hand
FakePlayerActions.of(bot).drop().execute();

// Drop off hand
FakePlayerActions.of(bot).drop(Hand.OFF_HAND, false).execute();

// Drop entire stack
FakePlayerActions.of(bot).drop(Hand.MAIN_HAND, true).execute();
```

### CLOSE_GUI

```java
FakePlayerActions.of(bot).closeGui().execute();
```

Executes the full `onClosed` lifecycle.

### USE_ITEM

```java
FakePlayerActions.of(bot).useItem(Hand.MAIN_HAND).execute();
```

### USE

Right-click a block (place, open door, lever, workbench, etc.).

```java
FakePlayerActions.of(bot).use(new BlockPos(100, 64, 200), Direction.UP).execute();
```

### INTERACT_ENTITY

```java
FakePlayerActions.of(bot).interact(villager, Hand.MAIN_HAND).execute();
```

### GUI_CLICK

Container slot click using native `ScreenHandler.onSlotClick` logic.

```java
// Requires direct Action construction
new GuiClickAction(bot, tick, slot, button, slotActionType);
```

::: warning
`clickSlot()` convenience method is not on the facade. Requires direct construction and submitting via `scheduler().runNow()`.
:::

### USE_RELEASE

Hold item then release (bow full charge, eat, shield block).

```java
// Requires direct Action construction
new UseReleaseAction(bot, tick, hand, duration);
```

::: warning
`useRelease()` convenience method is not on the facade. Requires direct construction and submitting via `scheduler().runNow()`.
:::

## FakePlayerActions Facade Methods

### Behavior Methods

| Method | Description |
|---|---|
| `attack(Entity)` | Attack entity |
| `drop()` | Drop main hand item |
| `drop(Hand, boolean)` | Drop specified hand item |
| `closeGui()` | Close container |
| `useItem(Hand)` | Use item |
| `dig(BlockPos, Direction)` | Dig block |
| `use(BlockPos, Direction)` | Right-click interact |
| `interact(Entity, Hand)` | Entity interact |
| `execute(Action)` | Execute custom Action |
| `pipeline(Action)` | Create ActionPipeline |

### Control Shortcuts

| Method | Description |
|---|---|
| `moveTo(BlockPos)` | Move to position |
| `lookAt(BlockPos)` | Look at position |
| `sneak()` | Sneak |
| `sprint()` | Sprint |
| `jump()` | Jump |
| `teleportTo(BlockPos)` | Teleport |
| `moveToPath(List<BlockPos>)` | Waypoint movement |
| `getHeldItem(Hand)` | Get held item |
| `swapHands()` | Swap hands |
| `setHeldItem(Hand, ItemStack)` | Set held item |
| `giveItem(ItemStack)` | Give item |
| `mount(Entity)` | Mount |
| `dismount()` | Dismount |
| `executeCommand(String)` | Execute command |
| `sendChatMessage(String)` | Send chat |
| `playSound(SoundEvent)` | Play sound |
| `setGameMode(GameMode)` | Set game mode |
| `setHealth(double)` | Set health |
| `setFoodLevel(int)` | Set food level |
| `addExperience(int)` | Add experience |
| `pathfindTo(BlockPos)` | Straight-line pathfind |
| `getNearbyContainers(double)` | Get nearby containers |
| `interactBlock(BlockPos, Direction, Hand)` | Block interact |
| `scanContainers(int)` | Scan containers |

## Action Scheduler

Actions are managed by `ActionScheduler` with three queue modes:

- **Immediate** (`runNow`) — Execute immediately, bypassing queue
- **Scheduled** (`scheduleAt`) — Execute at specified tick
- **Sequential** (`enqueue`) — Execute in queue order

Each bot's queue limit is `maxQueueLength` (default 64), concurrent stateful actions limit is `maxConcurrentActions` (default 16).

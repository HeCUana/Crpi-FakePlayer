# Navigation API

Navigation API is the most complex subsystem of CRPI-FakePlayer, providing A* pathfinding with physics-native execution.

```java
NavigationManager nav = bot.navigation();
```

## Quick Start

```java
// Pathfind to target
boolean success = nav.gotoBlock(new BlockPos(200, 64, 300));

// Arrive within radius
boolean success = nav.gotoNear(new BlockPos(200, 64, 300), 5);

// Multi-target pathfinding
boolean success = nav.gotoAny(
    new BlockPos(200, 64, 300),
    new BlockPos(210, 64, 310)
);

// Follow entity
nav.follow(targetEntity, 3);
```

## Core Methods

| Method | Return | Description |
|---|---|---|
| `gotoBlock(BlockPos)` | `boolean` | A* pathfind to exact position |
| `gotoNear(BlockPos, int)` | `boolean` | Arrive within radius |
| `gotoAny(BlockPos...)` | `boolean` | Arrive at any target |
| `follow(Entity)` | `void` | Continuously follow entity |
| `follow(Entity, int)` | `void` | Follow entity (specify distance) |
| `followPath(List<BlockPos>)` | `void` | Move through waypoints |
| `stop()` | `void` | Stop navigation |
| `pause()` | `void` | Pause navigation |
| `resume()` | `void` | Resume navigation |
| `repath()` | `void` | Force repath |
| `status()` | `NavigationStatus` | Get navigation status |
| `isNavigating()` | `boolean` | Is navigating |
| `isFinished()` | `boolean` | Is finished |
| `goal()` | `Goal` | Current goal |
| `currentPath()` | `Path` | Current path |
| `repaths()` | `int` | Repath count |

## NavigationStatus Enum

| Status | Description |
|---|---|
| `IDLE` | Idle |
| `CALCULATING` | Calculating path |
| `RUNNING` | Executing path |
| `SUCCESS` | Reached goal |
| `FAILED` | Pathfinding failed |
| `STUCK` | Stuck |
| `CANCELLED` | Cancelled |

## Goal System

### GoalBlock — Exact Target

```java
nav.gotoBlock(new BlockPos(100, 64, 200));
```

### GoalNear — Radius Target

```java
nav.gotoNear(new BlockPos(100, 64, 200), 5);
```

### GoalComposite — Composite Target

```java
// ANY_OF: succeed when any target is reached
nav.gotoAny(target1, target2, target3);
```

::: warning
`GoalComposite.ALL_OF` is practically unusable (requires a single point satisfying all sub-goals).
:::

### GoalFollow — Entity Following

```java
nav.follow(targetEntity, 3);
```

Continuous following with auto-repath when target moves, stops when target disappears.

## 8 Movement Types

| Type | Description | Condition |
|---|---|---|
| `MovementTraverse` | Flat 1-block walk | Target passable |
| `MovementAscend` | Up 1 block (jump) | Space above target |
| `MovementDescend` | Down 1 block | Support below |
| `MovementDiagonal` | Diagonal move | Both adjacent straight positions passable |
| `MovementFall` | Safe 2-3 block fall | Per-layer safety validation |
| `MovementParkour` | Sprint-jump across 1-2 block gaps | Requires sprint |
| `MovementBreak` | Mine soft blocks to clear path | Hardness ≤ 1.5 |
| `MovementPlace` | Place block to fill gaps | Blocks in inventory |

All movement bridges to Carpet Action Pack via `FakePlayerMovementController` (`setForward`/`setStrafing`/`setSneaking`/`setSprinting`/jump). The fake player's own tick consumes these inputs and Minecraft physics performs actual movement.

::: danger Do Not Teleport
Do not use `player.input` or `setPosition` — the fake player has no network input, and position-warping defeats the physics-native design.
:::

## NavigationProfile

Configuration file controlling pathfinding behavior:

```java
NavigationProfile profile = nav.profile();

// Properties
profile.allowBreak;        // Allow breaking blocks (default true)
profile.allowPlace;        // Allow placing blocks (default true)
profile.allowParkour;      // Allow parkour (default true)
profile.allowSprint;       // Allow sprinting (default true)
profile.allowSwim;         // Allow swimming (default false)
profile.maxFallDistance;   // Max fall distance (default 3)
profile.maxSearchRadius;   // Max search radius (default 128)
profile.maxCalculationBudgetNanos;  // Calculation budget (default 20ms)
```

### Custom Profile

```java
NavigationProfile custom = new NavigationProfile();
custom.allowSwim = true;
custom.maxFallDistance = 5;
nav.setProfile(custom);
```

## Cost Model

`CostModel` defines pathfinding costs for dangerous blocks:

| Danger Type | Cost |
|---|---|
| Lava | 100,000 |
| Fire | 1,000 |
| Cactus | 500 |
| 3×3 danger ring | Additional penalty |

`Favoring` applies a penalty to stuck positions during repathing, steering paths away from problem areas.

## Engine Behavior

### Search Constraints

- Max nodes: 1000
- Search radius: 128 blocks (from NavigationProfile)
- Calculation budget: 20ms (returns partial path if exceeded)
- Only plans over loaded chunks (never force-loads)

### Self-Healing

1. **Stuck detection** — 40 ticks with no significant movement → auto-repath
2. **Path invalidation** — World changes block path → auto-repath with position penalty
3. **Max repaths** — 3 times max (continuous `GoalFollow` resets counter each re-plan)

### Execution Flow

```
Goal → A* calculation → Path (Movement list) → PathExecutor step-by-step
  → Complete → SUCCESS
  → Stuck → Repath (max 3 times) → STUCK
  → Fail → FAILED
```

## Commands

```
/crpi fp goto <player> <pos> [near <radius>]    # A* pathfind
/crpi fp gotoany <player> <pos1> <pos2>          # Multi-target pathfind
/crpi fp follow <player> <entity>                # Follow entity
/crpi fp followpath <player> <x,z> [...]         # Waypoint path
/crpi fp navstop <player>                        # Stop navigation
/crpi fp navstatus <player>                      # Navigation status
```

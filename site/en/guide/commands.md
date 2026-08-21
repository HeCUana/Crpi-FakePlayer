# Commands Reference

All commands are under `/crpi fp`, requiring OP level 2 permission.

## Action Commands

### Attack

```
/crpi fp attack <player> <entity>
```

Makes the fake player attack the specified entity. Uses native damage/knockback/enchantment/cooldown logic.

**Parameters:**
- `player` — Fake player name
- `entity` — Target entity selector (e.g., `@e[type=zombie,limit=1,sort=nearest]`)

**Example:**
```
/crpi fp attack MyBot @e[type=zombie,limit=1,sort=nearest]
```

### Dig

```
/crpi fp dig <player> <pos> [face]
```

Digs the block at the specified position. Uses native mining hardness/tool/enchantment/water penalty formula.

**Parameters:**
- `player` — Fake player name
- `pos` — Block coordinates (`x y z`)
- `face` — Mining face (default `up`): `up` / `down` / `north` / `south` / `east` / `west`

**Example:**
```
/crpi fp dig MyBot 100 64 200 up
```

### Use Item

```
/crpi fp useitem <player> [off]
```

Uses the item in main hand (or off hand).

**Parameters:**
- `player` — Fake player name
- `off` — Optional, use off hand

**Example:**
```
/crpi fp useitem MyBot
/crpi fp useitem MyBot off
```

### Right-Click Interact

```
/crpi fp use <player> <pos> [face]
```

Right-clicks a block (place, open door, lever, workbench, etc.).

### Entity Interact

```
/crpi fp interact <player> <entity> [off]
```

Interacts with an entity (villager trade, ride, feed, etc.).

### Hold and Release

```
/crpi fp userelease <player> [off] [tick]
```

Holds item then releases (bow full charge, eat, shield block).

**Parameters:**
- `player` — Fake player name
- `off` — Optional, use off hand
- `tick` — Optional, hold duration in ticks (default 20)

### Drop Item

```
/crpi fp drop <player> [off|all]
```

- No argument: drop main hand item
- `off`: drop off hand item
- `all`: drop all items

### Close GUI

```
/crpi fp close <player>
```

Closes the currently open container/interface.

---

## Control Commands

### Move

```
/crpi fp move <player> <pos> [speed]
```

Moves to the specified position. Uses physics-native movement (not teleportation).

### Look At

```
/crpi fp lookat <player> <pos>
```

Makes the fake player look at the specified position (smooth 2-tick turn).

### Jump

```
/crpi fp jump <player>
```

Performs a jump.

### Teleport

```
/crpi fp teleport <player> <pos>
```

Safe teleport to the specified position (validates landing safety: solid below, air above).

### Sneak

```
/crpi fp sneak <player> [off]
```

Toggle sneak. Add `off` to cancel.

### Sprint

```
/crpi fp sprint <player> [off]
```

Toggle sprint. Add `off` to cancel.

### Swap Hands

```
/crpi fp swap <player>
```

Swap main hand and off hand items.

### Execute Command

```
/crpi fp exec <player> <command>
```

Execute command as the fake player (uses fake player's position/world/permissions).

**Example:**
```
/crpi fp exec MyBot tp ~ ~10 ~
/crpi fp exec MyBot give @s diamond 64
```

---

## Navigation Commands

### A* Pathfinding

```
/crpi fp goto <player> <pos> [near <radius>]
```

A* pathfind to the target position.

**Parameters:**
- `pos` — Target coordinates
- `near <radius>` — Optional, success within radius (default: exact match)

**Example:**
```
/crpi fp goto MyBot 200 64 300
/crpi fp goto MyBot 200 64 300 near 5
```

### Multi-Target Pathfinding

```
/crpi fp gotoany <player> <pos1> <pos2> ...
```

Pathfind to any one of the reachable targets.

### Follow Entity

```
/crpi fp follow <player> <entity>
```

Continuously follows the specified entity. Auto-repaths when target moves, stops when target disappears.

### Waypoint Path

```
/crpi fp followpath <player> <x,z> [<x,z> ...]
```

Move through waypoints in order (same-height only).

**Example:**
```
/crpi fp followpath MyBot 100,200 150,200 150,250
```

### Stop Navigation

```
/crpi fp navstop <player>
```

Stop current navigation.

### Navigation Status

```
/crpi fp navstatus <player>
```

View navigation status: `IDLE` / `CALCULATING` / `RUNNING` / `SUCCESS` / `FAILED` / `STUCK` / `CANCELLED`.

---

## Container Commands

### Container Operations

```
/crpi fp gui info <player>       # Current container info
/crpi fp gui list <player>       # Container slot list
/crpi fp gui click <player> <slot> [button] [mode]  # Click slot
/crpi fp gui close <player>      # Close container
```

::: tip
Container operations require opening the container first via `/crpi fp use`, then executing GUI commands.
:::

### Scan Containers

```
/crpi fp scancontainers <player> <radius>
```

Scans containers within the specified radius around the fake player.

---

## Other Commands

### List Fake Players

```
/crpi fp list
```

Lists all online fake players.

### Fake Player Info

```
/crpi fp info <player>
```

View detailed fake player status: position, dimension, game mode, current interface, etc.

# Getting Started

This guide will help you get a fake player up and running in 5 minutes.

## Prerequisites

| Dependency | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.19.3 |
| Carpet | 1.4.194 |
| Java | 21 |

## Step 1: Install

1. Download the latest `crpi-fakeplayer-x.x.x.jar`
2. Place the jar file in your server's `mods/` directory
3. Ensure Fabric API and Carpet jars are also in `mods/`
4. Start the server

See [Installation](./installation.md) for details.

## Step 2: Spawn a Fake Player

In the server console or in-game:

```
/player MyBot spawn
```

This spawns a fake player named `MyBot` at your current position using Carpet.

## Step 3: Execute Your First Behavior

### Using Commands

```
/crpi fp dig MyBot 100 64 200 up
```

Makes the fake player dig the block at coordinates `(100, 64, 200)`.

### Using API (via MCDR or other server mods)

```java
// Get fake player handle
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// Dig a block
FakePlayerActions.of(bot)
    .dig(new BlockPos(100, 64, 200), Direction.UP)
    .execute();
```

## Step 4: More Behaviors

```java
// Attack entity
FakePlayerActions.of(bot).attack(targetEntity).execute();

// Drop item
FakePlayerActions.of(bot).drop().execute();

// Move to position
bot.control().moveTo(new BlockPos(150, 64, 200), 1.0);

// A* pathfind
bot.navigation().gotoBlock(new BlockPos(200, 64, 300));

// Follow entity
bot.navigation().follow(targetEntity, 3);
```

## Next Steps

- [Commands](./commands.md) — All `/crpi fp` commands
- [Action API](/en/api/actions.md) — 9 behavior types in depth
- [Control API](/en/api/control.md) — Movement, look, inventory, etc.
- [Navigation API](/en/api/navigation.md) — A* pathfinding with physics-native execution
- [Configuration](/en/config.md) — Adjust mod behavior

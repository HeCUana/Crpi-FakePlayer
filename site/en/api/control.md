# Control API

Control API is accessed via `FakePlayerHandle`'s `control()` method, providing movement, look, state, inventory, riding, and command execution.

```java
FakePlayerControl ctrl = bot.control();
```

## Movement & Look

### moveTo — Move to Position

```java
ActionResult result = ctrl.moveTo(new BlockPos(100, 64, 200), 1.0);
```

- Physics-native movement (via Carpet Action Pack's `setForward`/`setStrafing`)
- Returns: `PASS` while moving, `SUCCESS` on arrival, `FAIL` on obstacle/timeout/stuck
- 0.5 block tolerance
- Per-tick stepping + collision detection + 20-tick stuck detection + 600-tick timeout

### lookAt — Look at Position

```java
ctrl.lookAt(new BlockPos(100, 64, 200));  // Look at block
ctrl.lookAt(targetEntity);                 // Look at entity
```

Smooth 2-tick turn.

### pathfindTo — Straight-Line Pathfind

```java
ctrl.pathfindTo(new BlockPos(100, 64, 200));
```

Straight-line version (for real A* pathfinding, use Navigation API).

## State Control

### sneak

```java
ctrl.sneak(true);   // Start sneaking
ctrl.sneak(false);  // Cancel sneaking
```

### sprint

```java
ctrl.sprint(true);   // Start sprinting
ctrl.sprint(false);  // Cancel sprinting
```

### jump

```java
ctrl.jump();
```

### teleportTo — Safe Teleport

```java
ctrl.teleportTo(new BlockPos(100, 64, 200));
```

Safety validation:
- Below must be solid block
- Teleport point and above must be air
- Auto-dismount
- Clears current move tasks

## Inventory

### getHeldItem

```java
ItemStackSnapshot mainHand = ctrl.getHeldItem(Hand.MAIN_HAND);
ItemStackSnapshot offHand = ctrl.getHeldItem(Hand.OFF_HAND);

String itemId = mainHand.itemId();  // e.g., "minecraft:diamond_pickaxe"
int count = mainHand.count();
```

`ItemStackSnapshot` is an immutable snapshot.

### swapHands

```java
ctrl.swapHands();
```

### setHeldItem

```java
ctrl.setHeldItem(Hand.MAIN_HAND, itemStack);
```

### giveItem

```java
ctrl.giveItem(new ItemStack(Items.DIAMOND, 64));
```

Uses native item stacking logic, won't overwrite existing items.

## Riding

### mount

```java
ctrl.mount(entity);
```

### dismount

```java
ctrl.dismount();
```

### startRiding

```java
ctrl.startRiding(entity, force);
```

## Commands & Expression

### executeCommand — Execute as Fake Player

```java
ctrl.executeCommand("tp ~ ~10 ~");
ctrl.executeCommand("give @s diamond 64");
```

Executes with the fake player's position, world, and permissions.

### sendChatMessage

```java
ctrl.sendChatMessage("Hello, world!");
```

Broadcasts a chat message as the fake player.

### playSound

```java
ctrl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
```

## Environment Queries

### getContainerInfo

```java
ContainerInfo info = ctrl.getContainerInfo();
if (info != null) {
    // Container type, position, etc.
}
```

### getNearbyContainers

```java
List<ContainerInfo> containers = ctrl.getNearbyContainers(16.0);
```

### interactBlock

```java
ctrl.interactBlock(new BlockPos(100, 64, 200), Direction.UP, Hand.MAIN_HAND);
```

Full-parameter block interaction.

## Property Setters

```java
ctrl.setGameMode(GameMode.SURVIVAL);
ctrl.setHealth(20.0);
ctrl.setFoodLevel(20);
ctrl.addExperience(100);
```

## Single-Task Model

::: warning Important
Each fake player can only have one active move/look task at a time. A new task automatically cancels the old one.
:::

- Movement task: per-tick position stepping + `blocksMovement()` collision detection
- Stuck detection: 20 ticks with no significant movement → fail
- Timeout: 600 ticks (30 seconds) → fail

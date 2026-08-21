# ActionPipeline

ActionPipeline is a behavior orchestration combinator introduced in v0.4.0, adding preconditions, timeout, retry, and event callbacks to Actions.

## Quick Start

```java
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .timeout(100)  // 100 tick timeout
    .retry(3)      // Retry 3 times on failure
    .onSuccess(r -> bot.control().sendChatMessage("Done!"))
    .onFailure(r -> bot.control().sendChatMessage("Failed..."))
    .execute(bot);
```

## API

### require — Preconditions

```java
.require(Predicate<Action> predicate)
```

Evaluated before execution. Condition not met → result is `SKIP`.

```java
// Check inventory for iron pickaxe
.require(HasItem.of("minecraft:iron_pickaxe"))

// Check inventory for at least 64 cobblestone
.require(HasItem.of("minecraft:cobblestone", 64))

// Custom condition
.require(action -> action.getHandle().getHealth() > 10.0)
```

### timeout — Timeout Control

```java
.timeout(int ticks)
```

Tick budget for stateful behaviors (DIG, USE_RELEASE). Timeout → result is `ABORT`.

```java
.timeout(100)  // 100 tick (5 second) timeout
```

### retry — Failure Retry

```java
.retry(int maxRetries)
```

Re-attempt on `FAIL` or `SKIP`. Hard limit: `MAX_PIPELINE_RETRIES = 8`.

```java
.retry(3)  // Max 3 retries
```

### onSuccess — Success Callback

```java
.onSuccess(Consumer<ActionResult> handler)
```

Triggered on terminal `SUCCESS`.

```java
.onSuccess(result -> {
    bot.control().sendChatMessage("Success!");
    bot.control().jump();
})
```

### onFailure — Failure Callback

```java
.onFailure(Consumer<ActionResult> handler)
```

Triggered on terminal non-`SUCCESS`.

```java
.onFailure(result -> {
    bot.control().sendChatMessage("Failed: " + result);
})
```

### execute — Execute

```java
.execute(FakePlayerHandle handle)
```

Submits the Pipeline to the fake player's ActionScheduler.

## HasItem Predicate

`HasItem` is a convenience precondition predicate checking if the fake player has specified items in inventory.

```java
// Check for iron pickaxe (any amount)
HasItem.of("minecraft:iron_pickaxe")

// Check for at least 64 cobblestone
HasItem.of("minecraft:cobblestone", 64)

// Check for diamond sword
HasItem.of("minecraft:diamond_sword")
```

### Usage Example

```java
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .execute(bot);
```

## Internal Mechanism

### PipelineRun

Each Pipeline execution creates a `PipelineRun` instance tracking:

- Current retry count
- Remaining timeout ticks
- Final result

### ActionHooks

Pipeline registers callbacks via `ActionHooks`:

- `beforeStart` — Evaluate preconditions before execution
- `afterEnd` — Trigger success/failure callbacks after execution
- `tick` — Check timeout each tick

### State Machine

```
Pipeline created
  → require() evaluation
    → Condition not met → SKIP
    → Condition met → Execute Action
      → Timeout → ABORT
      → Success → onSuccess callback
      → Failure → retry?
        → Has retries → Re-execute
        → No retries → onFailure callback
```

## Full Example

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// Dig diamond ore, needs iron pickaxe, 5s timeout, retry 2 times
new ActionPipeline<>(new DigAction(bot, tick, diamondOrePos, Direction.UP))
    .require(HasItem.of("minecraft:iron_pickaxe"))
    .timeout(100)
    .retry(2)
    .onSuccess(result -> {
        bot.control().sendChatMessage("Got the diamond ore!");
    })
    .onFailure(result -> {
        bot.control().sendChatMessage("Dig failed: " + result);
        // Give a new pickaxe
        bot.control().executeCommand("give @s iron_pickaxe");
    })
    .execute(bot);
```

## Limitations

- Max retry count: 8 (`MAX_PIPELINE_RETRIES`)
- `timeout` only effective for stateful behaviors (DIG, USE_RELEASE)
- Pipeline does not support chain execution (use `ActionSequence`, not yet implemented)

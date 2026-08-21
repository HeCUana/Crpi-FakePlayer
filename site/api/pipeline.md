# ActionPipeline

ActionPipeline 是 v0.4.0 引入的行为编排组合器，为 Action 添加前置条件、超时、重试和事件回调。

## 快速开始

```java
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .timeout(100)  // 100 tick 超时
    .retry(3)      // 失败重试 3 次
    .onSuccess(r -> bot.control().sendChatMessage("挖完了!"))
    .onFailure(r -> bot.control().sendChatMessage("失败了..."))
    .execute(bot);
```

## API

### require — 前置条件

```java
.require(Predicate<Action> predicate)
```

在执行前评估。条件不满足 → 结果为 `SKIP`。

```java
// 检查背包中有铁镐
.require(HasItem.of("minecraft:iron_pickaxe"))

// 检查背包中有至少 64 个圆石
.require(HasItem.of("minecraft:cobblestone", 64))

// 自定义条件
.require(action -> action.getHandle().getHealth() > 10.0)
```

### timeout — 超时控制

```java
.timeout(int ticks)
```

有状态行为（DIG、USE_RELEASE）的 Tick 预算。超时 → 结果为 `ABORT`。

```java
.timeout(100)  // 100 tick (5 秒) 超时
```

### retry — 失败重试

```java
.retry(int maxRetries)
```

在 `FAIL` 或 `SKIP` 时重新尝试。硬上限：`MAX_PIPELINE_RETRIES = 8`。

```java
.retry(3)  // 最多重试 3 次
```

### onSuccess — 成功回调

```java
.onSuccess(Consumer<ActionResult> handler)
```

在终端 `SUCCESS` 时触发。

```java
.onSuccess(result -> {
    bot.control().sendChatMessage("成功!");
    bot.control().jump();
})
```

### onFailure — 失败回调

```java
.onFailure(Consumer<ActionResult> handler)
```

在终端非 `SUCCESS` 时触发。

```java
.onFailure(result -> {
    bot.control().sendChatMessage("失败: " + result);
})
```

### execute — 执行

```java
.execute(FakePlayerHandle handle)
```

将 Pipeline 提交到假人的 ActionScheduler 执行。

## HasItem 谓词

`HasItem` 是一个便捷的前置条件谓词，检查假人背包中是否有指定物品。

```java
// 检查是否有铁镐（任意数量）
HasItem.of("minecraft:iron_pickaxe")

// 检查是否有至少 64 个圆石
HasItem.of("minecraft:cobblestone", 64)

// 检查是否有钻石剑
HasItem.of("minecraft:diamond_sword")
```

### 使用示例

```java
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .execute(bot);
```

## 内部机制

### PipelineRun

每个 Pipeline 执行时创建一个 `PipelineRun` 实例，跟踪：

- 当前重试次数
- 超时剩余 Tick
- 最终结果

### ActionHooks

Pipeline 通过 `ActionHooks` 注册回调：

- `beforeStart` — 执行前评估前置条件
- `afterEnd` — 执行后触发成功/失败回调
- `tick` — 每 Tick 检查超时

### 状态机

```
Pipeline 创建
  → require() 评估
    → 条件不满足 → SKIP
    → 条件满足 → 执行 Action
      → 超时 → ABORT
      → 成功 → onSuccess 回调
      → 失败 → retry?
        → 有重试次数 → 重新执行
        → 无重试次数 → onFailure 回调
```

## 完整示例

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// 挖掘钻石矿，需要铁镐，超时 5 秒，失败重试 2 次
new ActionPipeline<>(new DigAction(bot, tick, diamondOrePos, Direction.UP))
    .require(HasItem.of("minecraft:iron_pickaxe"))
    .timeout(100)
    .retry(2)
    .onSuccess(result -> {
        bot.control().sendChatMessage("挖到钻石矿了!");
    })
    .onFailure(result -> {
        bot.control().sendChatMessage("挖掘失败: " + result);
        // 换一把镐
        bot.control().executeCommand("give @s iron_pickaxe");
    })
    .execute(bot);
```

## 限制

- 最大重试次数：8（`MAX_PIPELINE_RETRIES`）
- `timeout` 仅对有状态行为（DIG、USE_RELEASE）有效
- Pipeline 不支持链式执行（使用 `ActionSequence`，尚未实现）

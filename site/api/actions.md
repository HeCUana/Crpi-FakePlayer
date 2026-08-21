# Action API

Action API 是 CRPI-FakePlayer 的核心行为系统，提供 9 种原生 Minecraft 行为类型。

## 快速开始

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// 使用 facade 方法
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
FakePlayerActions.of(bot).attack(entity).execute();
FakePlayerActions.of(bot).drop().execute();
```

## ActionType 枚举

| ActionType | 说明 | 执行方式 |
|---|---|---|
| `ATTACK` | 攻击实体 | 即时 |
| `DROP_ITEM` | 丢弃物品 | 即时 |
| `CLOSE_GUI` | 关闭容器 | 即时 |
| `USE_ITEM` | 使用物品 | 即时 |
| `DIG` | 挖掘方块 | 有状态（tick 驱动） |
| `USE` | 右键交互 | 即时 |
| `INTERACT_ENTITY` | 实体交互 | 即时 |
| `GUI_CLICK` | 容器槽位点击 | 即时 |
| `USE_RELEASE` | 长按释放 | 有状态（tick 驱动） |

## ActionResult 枚举

| 结果 | 说明 |
|---|---|
| `SUCCESS` | 执行成功 |
| `PASS` | 跳过（前置条件不满足） |
| `FAIL` | 执行失败 |
| `RETRY` | 需要重试 |
| `SKIP` | 跳过 |
| `ABORT` | 中止（超时等） |
| `INVALID_TARGET` | 目标无效 |
| `OUT_OF_RANGE` | 超出范围 |
| `NO_PERMISSION` | 无权限 |
| `INVALID_STATE` | 状态无效 |
| `CONCURRENCY_LIMIT` | 并发限制 |

## ActionState 生命周期

```
CREATED → QUEUED → STARTED → RUNNING → SUCCESS
                                   → FAILED
                                   → CANCELLED
```

## 详细行为说明

### ATTACK — 攻击

攻击实体，使用原生伤害计算（包含附魔、击退、冷却）。

```java
// Facade
FakePlayerActions.of(bot).attack(targetEntity).execute();

// 命令
// /crpi fp attack MyBot @e[type=zombie,limit=1]
```

### DIG — 挖掘

挖掘方块，使用原生挖掘公式（方块硬度、工具效率、附魔、水下惩罚）。

```java
// Facade
FakePlayerActions.of(bot).dig(new BlockPos(100, 64, 200), Direction.UP).execute();

// 命令
// /crpi fp dig MyBot 100 64 200 up
```

::: tip
DIG 是有状态行为，由 `ActionScheduler` 按 Tick 驱动，直到方块被破坏。
:::

### DROP_ITEM — 丢弃物品

```java
// 丢弃主手物品
FakePlayerActions.of(bot).drop().execute();

// 丢弃副手物品
FakePlayerActions.of(bot).drop(Hand.OFF_HAND, false).execute();

// 丢弃整个物品栈
FakePlayerActions.of(bot).drop(Hand.MAIN_HAND, true).execute();
```

### CLOSE_GUI — 关闭容器

```java
FakePlayerActions.of(bot).closeGui().execute();
```

执行完整的 `onClosed` 生命周期。

### USE_ITEM — 使用物品

```java
FakePlayerActions.of(bot).useItem(Hand.MAIN_HAND).execute();
```

### USE — 右键交互

右键点击方块（放置、开门、拉杆、打开工作台等）。

```java
FakePlayerActions.of(bot).use(new BlockPos(100, 64, 200), Direction.UP).execute();
```

### INTERACT_ENTITY — 实体交互

```java
FakePlayerActions.of(bot).interact(villager, Hand.MAIN_HAND).execute();
```

### GUI_CLICK — 容器槽位点击

使用原生 `ScreenHandler.onSlotClick` 逻辑。

```java
// 需要直接构造 Action
new GuiClickAction(bot, tick, slot, button, slotActionType);
```

::: warning
`clickSlot()` 便捷方法不在 facade 上，需要直接构造并通过 `scheduler().runNow()` 提交。
:::

### USE_RELEASE — 长按释放

长按物品后释放（弓满弦、进食、盾牌格挡）。

```java
// 需要直接构造 Action
new UseReleaseAction(bot, tick, hand, duration);
```

::: warning
`useRelease()` 便捷方法不在 facade 上，需要直接构造并通过 `scheduler().runNow()` 提交。
:::

## FakePlayerActions Facade 方法

### 行为方法

| 方法 | 说明 |
|---|---|
| `attack(Entity)` | 攻击实体 |
| `drop()` | 丢弃主手物品 |
| `drop(Hand, boolean)` | 丢弃指定手物品 |
| `closeGui()` | 关闭容器 |
| `useItem(Hand)` | 使用物品 |
| `dig(BlockPos, Direction)` | 挖掘方块 |
| `use(BlockPos, Direction)` | 右键交互 |
| `interact(Entity, Hand)` | 实体交互 |
| `execute(Action)` | 执行自定义 Action |
| `pipeline(Action)` | 创建 ActionPipeline |

### 控制快捷方法

| 方法 | 说明 |
|---|---|
| `moveTo(BlockPos)` | 移动到位置 |
| `lookAt(BlockPos)` | 看向位置 |
| `sneak()` | 潜行 |
| `sprint()` | 疾跑 |
| `jump()` | 跳跃 |
| `teleportTo(BlockPos)` | 传送 |
| `moveToPath(List<BlockPos>)` | 路径点移动 |
| `getHeldItem(Hand)` | 获取手持物品 |
| `swapHands()` | 交换双手 |
| `setHeldItem(Hand, ItemStack)` | 设置手持物品 |
| `giveItem(ItemStack)` | 给予物品 |
| `mount(Entity)` | 骑乘 |
| `dismount()` | 下马 |
| `executeCommand(String)` | 执行命令 |
| `sendChatMessage(String)` | 发送聊天 |
| `playSound(SoundEvent)` | 播放音效 |
| `setGameMode(GameMode)` | 设置游戏模式 |
| `setHealth(double)` | 设置生命值 |
| `setFoodLevel(int)` | 设置饥饿值 |
| `addExperience(int)` | 增加经验 |
| `pathfindTo(BlockPos)` | 直线寻路 |
| `getNearbyContainers(double)` | 获取附近容器 |
| `interactBlock(BlockPos, Direction, Hand)` | 方块交互 |
| `scanContainers(int)` | 扫描容器 |

## 行为调度器

行为通过 `ActionScheduler` 管理，支持三种队列模式：

- **立即执行** (`runNow`) — 跳过队列直接执行
- **定时执行** (`scheduleAt`) — 在指定 tick 执行
- **顺序执行** (`enqueue`) — 按队列顺序执行

每个假人的队列上限为 `maxQueueLength`（默认 64），并发有状态行为上限为 `maxConcurrentActions`（默认 16）。

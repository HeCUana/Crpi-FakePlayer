# API 概览

CRPI-FakePlayer 提供三个 API 表面，全部通过 `FakePlayerHandle` 访问。

## 架构红线

在使用 API 之前，请理解两条不可违反的架构约束：

1. **零 Mixin / 零新线程** — 所有行为通过原生 Minecraft 入口点执行（`ServerPlayerEntity.attack`、`dropItem`、`closeHandledScreen`、`ServerPlayerInteractionManager.interactBlock`/`interactItem`/`finishMining`、`ScreenHandler.onSlotClick`、`onStoppedUsing`/`finishUsing`）。伤害、附魔、冷却、挖掘公式、物品逻辑全部保持原生。
2. **Tick 驱动** — 所有持续性行为在服务端主线程上按 Tick 步进。不创建新线程，不使用异步回调。

## Tick 链路

```
Server tick → Carpet extension onTick (CRPIFakePlayerMod.onTick)
  → FakePlayerRegistry.tick    (清理断开连接的假人)
  → ActionScheduler.tick       (有状态行为: DIG / USE_RELEASE)
  → ControlManager.tick        (MoveTask / LookTask 持续任务)
  → NavigationRegistry.tick    (每假人 NavigationManager)
```

## 获取假人句柄

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");
```

`FakePlayerHandle` 是对 `ServerPlayerEntity` 的轻薄包装。Adapter 是唯一引用 Carpet 内部类（`EntityPlayerMPFake`）的地方。

## 三大 API 表面

### Action API

```java
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
```

9 种原生行为类型，即时执行或有状态执行。详见 [Action API](./actions.md)。

### Control API

```java
bot.control().moveTo(new BlockPos(100, 64, 200), 1.0);
bot.control().lookAt(targetEntity);
bot.control().sneak(true);
```

移动、视角、状态、背包、骑乘、命令执行。详见 [Control API](./control.md)。

### Navigation API

```java
bot.navigation().gotoBlock(new BlockPos(200, 64, 300));
bot.navigation().follow(targetEntity, 3);
```

A* 寻路 + 物理原生执行。详见 [Navigation API](./navigation.md)。

## ActionPipeline

```java
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .timeout(100)
    .retry(3)
    .onSuccess(r -> bot.control().sendChatMessage("完成!"))
    .execute(bot);
```

行为编排组合器。详见 [ActionPipeline](./pipeline.md)。

## 包结构

```
com.crpi.fakeplayer
├── action/          # 行为框架 (ActionType, Action, ActionDispatcher)
│   ├── impl/        # 9 种行为实现
│   └── executor/    # 行为执行器
├── api/             # 公共 API 表面
│   └── pred/        # 谓词 (HasItem)
├── command/         # /crpi fp 命令树
├── config/          # Carpet 规则
├── container/       # 容器扫描
├── control/         # 控制系统 (MoveTask, LookTask)
├── fakeplayer/      # 句柄适配器
├── itemuse/         # 物品使用会话
├── mining/          # 挖掘会话
├── navigation/      # 寻路引擎
│   ├── cost/        # 代价模型
│   ├── executor/    # 路径执行器
│   ├── goal/        # 目标类型
│   ├── movement/    # 8 种移动类型
│   ├── pathfinding/ # A* 算法
│   └── world/       # 世界抽象
└── scheduler/       # 行为调度器
```

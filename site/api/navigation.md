# Navigation API

Navigation API 是 CRPI-FakePlayer 最复杂的子系统，提供 A* 寻路引擎 + 物理原生执行。

```java
NavigationManager nav = bot.navigation();
```

## 快速开始

```java
// 寻路到目标位置
boolean success = nav.gotoBlock(new BlockPos(200, 64, 300));

// 在半径内到达即成功
boolean success = nav.gotoNear(new BlockPos(200, 64, 300), 5);

// 多目标寻路
boolean success = nav.gotoAny(
    new BlockPos(200, 64, 300),
    new BlockPos(210, 64, 310)
);

// 跟随实体
nav.follow(targetEntity, 3);
```

## 核心方法

| 方法 | 返回值 | 说明 |
|---|---|---|
| `gotoBlock(BlockPos)` | `boolean` | A* 寻路到精确位置 |
| `gotoNear(BlockPos, int)` | `boolean` | 到达半径内即成功 |
| `gotoAny(BlockPos...)` | `boolean` | 到达任意一个目标 |
| `follow(Entity)` | `void` | 持续跟随实体 |
| `follow(Entity, int)` | `void` | 跟随实体（指定距离） |
| `followPath(List<BlockPos>)` | `void` | 按路径点移动 |
| `stop()` | `void` | 停止导航 |
| `pause()` | `void` | 暂停导航 |
| `resume()` | `void` | 恢复导航 |
| `repath()` | `void` | 强制重新寻路 |
| `status()` | `NavigationStatus` | 获取导航状态 |
| `isNavigating()` | `boolean` | 是否正在导航 |
| `isFinished()` | `boolean` | 是否已完成 |
| `goal()` | `Goal` | 当前目标 |
| `currentPath()` | `Path` | 当前路径 |
| `repaths()` | `int` | 已重新寻路次数 |

## NavigationStatus 枚举

| 状态 | 说明 |
|---|---|
| `IDLE` | 空闲 |
| `CALCULATING` | 正在计算路径 |
| `RUNNING` | 正在执行路径 |
| `SUCCESS` | 到达目标 |
| `FAILED` | 寻路失败 |
| `STUCK` | 卡住 |
| `CANCELLED` | 被取消 |

## Goal 系统

### GoalBlock — 精确目标

```java
nav.gotoBlock(new BlockPos(100, 64, 200));
```

### GoalNear — 半径目标

```java
nav.gotoNear(new BlockPos(100, 64, 200), 5);
```

### GoalComposite — 复合目标

```java
// ANY_OF: 到达任意一个目标即成功
nav.gotoAny(target1, target2, target3);
```

::: warning
`GoalComposite.ALL_OF` 在实践中几乎不可用（需要找到同时满足所有子目标的单个点）。
:::

### GoalFollow — 实体跟随

```java
nav.follow(targetEntity, 3);
```

持续跟随，目标移动时自动重新寻路，目标消失则停止。

## 8 种移动类型

| 类型 | 说明 | 条件 |
|---|---|---|
| `MovementTraverse` | 平地行走 1 格 | 目标位置可通行 |
| `MovementAscend` | 向上 1 格（跳跃） | 目标上方有空间 |
| `MovementDescend` | 向下 1 格 | 下方有支撑 |
| `MovementDiagonal` | 对角移动 | 两个相邻直行位置均可通行 |
| `MovementFall` | 安全下落 2-3 格 | 每层验证安全性 |
| `MovementParkour` | 疾跑跳跃跨 1-2 格间隙 | 需要疾跑 |
| `MovementBreak` | 破坏软方块清除路径 | 方块硬度 ≤ 1.5 |
| `MovementPlace` | 放置方块填补间隙 | 背包中有方块 |

所有移动通过 `FakePlayerMovementController` 桥接到 Carpet Action Pack（`setForward`/`setStrafing`/`setSneaking`/`setSprinting`/jump），由假人自身的 Tick 消费这些输入，Minecraft 物理执行实际移动。

::: danger 不要使用传送
不要使用 `player.input` 或 `setPosition` — 假人没有网络输入，位置扭曲会破坏物理原生设计。
:::

## NavigationProfile

控制寻路行为的配置文件：

```java
NavigationProfile profile = nav.profile();

// 属性
profile.allowBreak;        // 允许破坏方块 (默认 true)
profile.allowPlace;        // 允许放置方块 (默认 true)
profile.allowParkour;      // 允许跑酷 (默认 true)
profile.allowSprint;       // 允许疾跑 (默认 true)
profile.allowSwim;         // 允许游泳 (默认 false)
profile.maxFallDistance;   // 最大下落距离 (默认 3)
profile.maxSearchRadius;   // 最大搜索半径 (默认 128)
profile.maxCalculationBudgetNanos;  // 计算预算 (默认 20ms)
```

### 自定义 Profile

```java
NavigationProfile custom = new NavigationProfile();
custom.allowSwim = true;
custom.maxFallDistance = 5;
nav.setProfile(custom);
```

## 代价模型

`CostModel` 定义了各种危险方块的寻路代价：

| 危险类型 | 代价 |
|---|---|
| 岩浆 | 100,000 |
| 火 | 1,000 |
| 仙人掌 | 500 |
| 3×3 危险环 | 额外惩罚 |

`Favoring` 在重新寻路时对卡住位置施加惩罚，引导路径绕开问题区域。

## 引擎行为

### 搜索约束

- 最大节点数：1000
- 搜索半径：128 格（来自 NavigationProfile）
- 计算预算：20ms（返回部分路径执行）
- 只在已加载区块内规划（不强制加载）

### 自愈机制

1. **卡住检测** — 40 tick 无显著位移 → 触发重新寻路
2. **路径失效** — 世界变化导致路径被阻挡 → 自动重新寻路 + 位置惩罚
3. **最大重新寻路** — 最多 3 次（持续 `GoalFollow` 每次重新规划重置计数器）

### 执行流程

```
Goal → A* 计算 → Path（Movement 列表）→ PathExecutor 逐步执行
  → 完成 → SUCCESS
  → 卡住 → 重新寻路（最多 3 次）→ STUCK
  → 失败 → FAILED
```

## 命令

```
/crpi fp goto <player> <pos> [near <radius>]    # A* 寻路
/crpi fp gotoany <player> <pos1> <pos2>          # 多目标寻路
/crpi fp follow <player> <entity>                # 跟随实体
/crpi fp followpath <player> <x,z> [...]         # 路径点导航
/crpi fp navstop <player>                        # 停止导航
/crpi fp navstatus <player>                      # 导航状态
```

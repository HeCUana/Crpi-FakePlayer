# CRPI-FakePlayer Navigation 系统 — 第一阶段分析报告

> 状态：**Phase 1-3 已实施并实测通过（2026-08-15）**。Phase 4-5 待实施。
> Phase 3 新增：MovementParkour（1-2 格缺口跨越）、CostModel（危险方块代价）、Favoring（失败位置惩罚）、动态路径失效检测（世界变化→自动重规划）。
> Phase 2 新增：MovementDiagonal（防斜穿墙角）、MovementFall（2-3 格安全下落，超限拒绝）、StandPositionFinder（可站立位置查找，供 InteractionPositionFinder 复用）。以下为项目扫描与设计分析，未写任何代码。

---

## 1. 当前项目结构

```
com.crpi.fakeplayer/
├── CRPIFakePlayerMod          # ModInitializer + CarpetExtension（onTick 驱动入口）
├── action/                    # Action 框架（Dispatcher/Executor/Scheduler，tick 驱动）
├── control/                   # 0.2.0 Control API
│   ├── FakePlayerControl      # 24 个控制 API（moveTo/lookAt/…）
│   ├── ControlTask/MoveTask/LookTask   # 持续任务（每 tick 步进）
│   └── ControlManager         # 每 tick 驱动所有任务
├── fakeplayer/                # FakePlayerHandle/Adapter/Registry（Carpet 适配）
├── scheduler/                 # ActionQueue/ActionScheduler
├── mining/ itemuse/ container/ # DIG/长按使用/容器（含 ContainerScanner 只读扫描）
├── command/                   # /crpi fp 命令（薄壳）
├── config/                    # 10 条 Carpet 规则
└── api/                       # FakePlayerActions 流畅 API
```

## 2. FakePlayer 控制链

```
命令/API
  → FakePlayerHandle.control() → FakePlayerControl
      → 持续任务（MoveTask/LookTask）→ ControlManager.tick（Carpet onTick 驱动）
      → 瞬时操作（sneak/jump/…）→ ServerPlayerEntity 原生方法
```

## 3. 当前 Tick 链

```
Server tick → Carpet onTick（CRPIFakePlayerMod.onTick）
  → ActionScheduler.tick（DIG/USE_RELEASE 等 Stateful Action）
  → ControlManager.tick（MoveTask/LookTask）
（全部服务器主线程，零新线程）
```

## 4. 当前 World / Collision API（1.21.11 已核实）

| 能力 | 现有代码 |
|---|---|
| 可通行判定 | `BlockState.blocksMovement()`（MoveTask 已用） |
| 区块检查 | `ServerWorld.isChunkLoaded(pos)`（不主动加载） |
| 碰撞形状 | `BlockState.getCollisionShape(BlockView, BlockPos)`（yarn 1.21.11 可用，需新增使用） |
| 液体检测 | `BlockState.getFluidState().isEmpty()`（需新增使用） |
| 危险方块 | `world.getBlockState(pos).getBlock()` instanceof Lava/Fire（需新增） |
| 站立判定 | 脚下 `blocksMovement` 下上两格可通行（MoveTask.isPassable 雏形，需扩展为完整 StandPositionFinder） |

## 5. 可以直接复用的模块

- `FakePlayerHandle` / `FakePlayerAdapter` —— 假人句柄与 Carpet 适配
- `ControlManager` tick 驱动模式 —— Navigation 执行器沿用同一 tick 入口
- `LookTask` —— 平滑转向（Navigation 的 look 控制可直接复用）
- `MoveTask` 的卡住/超时/碰撞检测思路 —— Watchdog/StuckDetector 的基础
- `ContainerScanner` —— 未来 GoalChest / 环境感知
- `ActionResult` / 规则系统 —— Navigation 失败原因映射
- **`EntityPlayerActionPack`（Carpet）** —— 已核实：`setForward(float)/setStrafing(float)/setSneaking/setSprinting/look()/stopMovement()/JUMP action`——这是假人移动的**官方机制**（`/player move` 命令同款，物理全原生：跳跃/掉落/碰撞/台阶）

## 6. 需要新增的模块（navigation/ 包）

```
navigation/
├── NavigationManager          # 入口：goto/gotoNear/follow/stop/status
├── goal/                      # Goal 接口 + GoalBlock/GoalNear/GoalXZ（Phase 1）
├── path/                      # PathNode/Path/PathSegment
├── pathfinding/               # AStarPathFinder/BinaryHeapOpenSet
├── movement/                  # Movement 接口 + MovementResult +
│                              #   Traverse/Ascend/Descend（Phase 1）
│                              #   Diagonal/Fall/Parkour（Phase 2-3）
├── world/                     # NavigationWorld（world 数据抽象：可通行/危险/碰撞/站立）
├── executor/                  # PathExecutor/MovementExecutor/NavigationTickExecutor
├── movement/controller/       # FakePlayerMovementController（Carpet action pack 适配层）
├── recovery/                  # Watchdog/StuckDetector/RepathManager（Phase 4）
└── interaction/               # InteractionPositionFinder（Phase 6）
```

## 7. 与 Baritone 架构的对应关系

| 本方案 | Baritone 对应 |
|---|---|
| NavigationManager | PathingBehavior（入口/状态机） |
| Goal（GoalBlock/GoalNear） | GoalBlock/GoalNear |
| AStarPathFinder + BinaryHeapOpenSet | AStarPathFinder + OpenSet |
| Movement（Traverse/Ascend/Descend） | Moves（MovementTraverse/Ascend/Descend） |
| Path + PathSegment | Path + Moves 列表 |
| PathExecutor | PathExecutor |
| NavigationWorld + WorldCache | BaritoneWorldDataProvider/IWorldReader 缓存 |
| Watchdog/StuckDetector | PathingBehavior 的 stuck 检测 |
| RepathManager + Favoring | dynamic repath + favor |
| FakePlayerMovementController | InputOverrideHandler（input 控制） |
| NavigationProfile | 设置开关（allowParkour/allowSprint 等） |

## 8. 关键设计决策（请确认）

1. **移动执行层**：提示词要求"禁止 setPosition、必须 forward/jump 等 input 方式"——已核实 Carpet 假人的官方 input 机制是 **`EntityPlayerActionPack`**（`getActionPack().setForward(1)` 等），`FakePlayerMovementController` 将包装它（不是原版 `player.input` 字段，假人无网络 input）。这意味着 **Navigation 的移动物理完全原生**（含跳跃/掉落/碰撞），与 0.2.0 MoveTask 的位置步进并存（MoveTask 保留为直线工具，Navigation 是升级路径）。
2. **寻路线程模型**：Phase 1-4 全部**同步**在主线程计算（限制：最大节点数 ~1000、最大搜索范围 128 格、计算超时保护 20ms 预算——超出即返回 PARTIAL_PATH 执行后继续），Phase 5 再做 WorldSnapshot + 后台线程异步寻路。
3. **Phase 1 范围**：`goto(BlockPos)` / `gotoNear` 可完整工作（平地+1 格上台阶+下落），Diagonal/Fall/Parkour/Danger 成本在 Phase 2-3 加入。
4. **不复制 Baritone 源码**：仅架构/算法思想参考，全部按本项目的 ActionResult/规则/命令风格重写。

## 9. 第一阶段具体修改文件列表

```
新增（约 14 个文件）:
navigation/NavigationManager.java
navigation/NavigationProfile.java
navigation/goal/Goal.java
navigation/goal/GoalBlock.java
navigation/goal/GoalNear.java
navigation/path/PathNode.java
navigation/path/Path.java
navigation/pathfinding/BinaryHeapOpenSet.java
navigation/pathfinding/AStarPathFinder.java
navigation/movement/Movement.java
navigation/movement/MovementResult.java
navigation/movement/MovementTraverse.java
navigation/movement/MovementAscend.java
navigation/movement/MovementDescend.java
navigation/world/NavigationWorld.java
navigation/movement/controller/FakePlayerMovementController.java
navigation/executor/PathExecutor.java
navigation/executor/NavigationTickExecutor.java

修改（3 个文件）:
CRPIFakePlayerMod.java      # onTick 挂 NavigationManager tick
FakePlayerHandle.java       # + navigation() 入口
FakePlayerCommand.java      # + /crpi fp goto/nav 调试命令
```

确认后按 Phase 1 → 2 → 3 → 4 实施，每阶段 build + Survival 测试服实测。

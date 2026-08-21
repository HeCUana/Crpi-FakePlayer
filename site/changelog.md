# 更新日志

## 0.4.0

**ActionPipeline 行为编排**

- 新增 `ActionPipeline` 组合器：前置条件、超时、重试、成功/失败回调
- 新增 `HasItem` 谓词：检查背包物品
- 新增 `ActionHooks`：行为生命周期钩子
- 新增 `PipelineRun`：Pipeline 执行状态跟踪

## 0.3.0

**Navigation 寻路系统**

- 新增 A* 寻路引擎（1000 节点上限、20ms 计算预算）
- 新增 8 种物理原生移动类型：Traverse、Ascend、Descend、Diagonal、Fall、Parkour、Break、Place
- 新增 Goal 系统：GoalBlock、GoalNear、GoalComposite、GoalFollow
- 新增 `FakePlayerMovementController`：桥接 Carpet Action Pack
- 新增 NavigationProfile：可配置寻路行为
- 新增 CostModel + Favoring：危险代价 + 位置惩罚
- 新增卡住检测 + 自动重新寻路（最多 3 次）

## 0.2.0

**Control 控制系统**

- 新增 `FakePlayerControl`：24+ 控制方法
- 新增 MoveTask / LookTask：持续移动和视角任务
- 新增物理原生移动（非传送）
- 新增安全传送验证
- 新增背包操作（getHeldItem、swapHands、setHeldItem、giveItem）
- 新增骑乘操作（mount、dismount）
- 新增命令执行（executeCommand、sendChatMessage）
- 新增环境查询（getContainerInfo、getNearbyContainers）

## V0.4

**USE_RELEASE 状态行为**

- 新增 `UseReleaseAction`：长按物品后释放
- 新增 `ItemUseSession`：物品使用状态机

## V0.3

**容器操作**

- 新增 `GuiClickAction`：容器槽位点击
- 新增 `ContainerContext` / `ContainerManager`：容器操作上下文
- 新增 `ContainerScanner`：容器扫描

## V0.2

**DIG / USE / INTERACT_ENTITY**

- 新增 `DigAction`：挖掘方块（原生硬度/工具/附魔公式）
- 新增 `UseAction`：右键交互
- 新增 `InteractEntityAction`：实体交互
- 新增 `MiningSession`：挖掘状态机

## V0.1

**框架 + 基础行为**

- 行为框架：`ActionType`、`Action`、`ActionDispatcher`、`ActionExecutor`
- `ActionScheduler`：行为调度器（即时/定时/顺序队列）
- `FakePlayerHandle` / `FakePlayerAdapter`：假人句柄适配
- `FakePlayerActions`：Fluent facade
- 基础行为：ATTACK、DROP_ITEM、CLOSE_GUI、USE_ITEM

---

## 未来规划

- `ActionSequence`：行为链式执行
- 游泳/水下寻路
- Replay/TAS 桥接
- AI 行为决策
- Litematica 桥接

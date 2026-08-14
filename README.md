# CRPI-FakePlayer

Carpt Fake Player 的服务端行为框架（Fabric + Carpet 附属 Mod）。通过统一的 Action API 驱动假人执行接近真实玩家的 Minecraft 行为，**纯服务端实现，客户端无需安装，零 Mixin**。

- Minecraft **1.21.11** / Fabric Loader 0.19.x / Carpet **1.4.194**
- 当前版本：**V0.4**（0.1.0）

## 依赖

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.x |
| Carpet Mod | 1.4.194 |
| Java | 21 |

## 安装

1. 将 `crpi-fakeplayer-0.1.0.jar` 放入服务器 `mods/`
2. 客户端**不需要**安装任何东西
3. 验证：`/crpi-fakeplayer list` 应列出 9 条规则

## 命令（V0.1）

所有命令挂载在 `/crpi fp` 下（与 CRPI Carpet 共存时自动挂到其 `crpi` 根；单独使用时自动创建根）：

```
/crpi fp list                      在线假人列表
/crpi fp info <玩家>               假人状态（位置/维度/模式/容器）
/crpi fp attack <玩家> <实体>      攻击实体（原生玩家攻击路径）
/crpi fp drop <玩家> [off|all]     丢物品（主手/副手/整组）
/crpi fp close <玩家>              关闭打开的容器
/crpi fp useitem <玩家> [off]      使用手中物品
/crpi fp dig <玩家> <坐标> [面]    挖掘方块（状态机：RUNNING→SUCCESS）
/crpi fp use <玩家> <坐标> [面]    右键方块（放置/开箱/按钮/门）
/crpi fp interact <玩家> <实体> [off]  与实体交互（村民/骑乘/喂食）
/crpi fp gui info <玩家>             当前容器信息（类型/syncId/槽位数）
/crpi fp gui list <玩家>             容器内容列表
/crpi fp gui click <玩家> <槽位> <按钮> <操作>  槽位点击（pickup/quick_move/swap/clone/throw/pickup_all）
/crpi fp gui close <玩家>            关闭容器
/crpi fp userelease <玩家> [off] [tick]  长按物品 tick 后释放（弓/食物/盾）
/crpi fp scancontainers <玩家> <半径>   扫描范围内容器（只读，不开箱不加载区块）
```

要求 OP 权限（level 2）。

## Carpet 规则

| 规则 | 默认 | 说明 |
|---|---|---|
| `fakePlayerActions` | true | 总开关 |
| `fakePlayerCombat` | true | ATTACK |
| `fakePlayerItemUse` | true | USE_ITEM / USE_RELEASE |
| `fakePlayerInteraction` | true | USE / INTERACT_ENTITY |
| `fakePlayerMining` | true | DIG |
| `fakePlayerContainer` | true | GUI_CLICK 等容器操作 |
| `fakePlayerDebug` | false | Action 详细日志 |
| `maxQueueLength` | 64 | 每假人队列上限 |
| `maxConcurrentActions` | 16 | 并发 Stateful Action 上限 |
| `maxContainerScanRadius` | 16 | 容器扫描最大半径 |

## Action API（V0.1）

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

FakePlayerActions.of(bot)
    .attack(zombie)
    .execute();

FakePlayerActions.of(bot)
    .drop(Hand.MAIN_HAND, false)
    .execute();

FakePlayerActions.of(bot)
    .useItem(Hand.MAIN_HAND)
    .execute();

FakePlayerActions.of(bot)
    .closeGui()
    .execute();
```

所有行为走 Minecraft 原生玩家入口：`ServerPlayerEntity.attack` / `dropItem` / `closeHandledScreen`、`ServerPlayerInteractionManager.interactItem`——伤害、附魔、冷却、物品逻辑全部原生。

## 架构

```
Action (参数+状态+结果)
   ↓
ActionDispatcher (type → executor)
   ↓
ActionScheduler (每 server tick 驱动，无额外线程)
   ↓
ActionExecutor → FakePlayerHandle → ServerPlayerEntity 原生 API
```

```
com.crpi.fakeplayer/
├── action/          Action 抽象、类型、状态、结果、分发器
│   ├── impl/        各动作参数类
│   └── executor/    各动作执行器（零 Mixin，纯原生 API）
├── fakeplayer/      Carpet 假人适配层（EntityPlayerMPFake → Handle）
├── scheduler/       Tick 调度 + 每假人队列
├── command/         /crpi fp 调试命令（薄壳）
├── config/          Carpet 规则
└── api/             FakePlayerActions 流畅 API
```

## 版本路线

| 版本 | 内容 | 状态 |
|---|---|---|
| V0.1 | 框架 + ATTACK / DROP_ITEM / CLOSE_GUI / USE_ITEM | ✅ 已实测 |
| V0.2 | DIG（MiningSession）/ USE / INTERACT_ENTITY | ✅ 已实测 |
| V0.3 | 容器操作：GUI_CLICK / ContainerContext | ✅ 已实测 |
| V0.4 | Stateful：USE_RELEASE / ItemUseSession | ✅ 已实测 |

## 测试状态（Survival 测试服 RCON 实测）

- ATTACK：僵尸 20.0 → 19.06 实际掉血 ✅
- DIG：creative 即时破坏 / survival 渐进挖掘（原生 calcBlockBreakingDelta 累积 + finishMining）✅
- USE：开箱（screen 切换验证）/ 按钮 / 放置 ✅
- INTERACT_ENTITY：村民交互 ✅
- GUI_CLICK：苹果 箱子→PICKUP→玩家背包→QUICK_MOVE→箱子 全链路 ✅（原生 onSlotClick）
- USE_RELEASE：弓 40 tick 满力射箭 ✅（原生 onStoppedUsing）；食物 finishUsing 消耗 ✅
- CONTAINER_SCAN：9 种容器扫描（含锁/距离判定 canOpen、物品聚合、双箱去重）✅
- 容器校验：非法槽位 INVALID_TARGET / 无容器 INVALID_STATE ✅
- DROP_ITEM：主手 / 整组丢弃 ✅
- USE_ITEM：假人吃苹果 ✅
- CLOSE_GUI：无容器时正确返回 INVALID_STATE ✅
- 与 CRPI Carpet 共存（crpi 根挂载无冲突）✅
- 9 条规则注册 ✅

## 已知限制（V0.1）


- 队列仅实现基础（延迟/顺序），ActionSequence 链式 API 未实现
- 容器打开后假人屏幕状态同步依赖 Carpet 假人的 FakeClientConnection 行为

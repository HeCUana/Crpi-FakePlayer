# CRPI-FakePlayer

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=flat-square)]()
[![Carpet](https://img.shields.io/badge/Carpet-1.4.194-4A90D9?style=flat-square)]()
[![Mod Loader](https://img.shields.io/badge/Loader-Fabric-8b6b9c?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)]()
[![Version](https://img.shields.io/badge/Version-0.1.0-orange?style=flat-square)]()

Carpet Fake Player 的服务端行为框架（Fabric + Carpet 附属 Mod）。通过统一的 Action API 驱动假人执行接近真实玩家的 Minecraft 行为。

- Minecraft **1.21.11** / Fabric Loader 0.19.x / Carpet **1.4.194**
- 当前版本：**0.1.0**（V0.1~V0.4 功能集）

## 重要

- **本 Mod 只驱动 Carpet 假人**（`/player <名字> spawn` 生成的假人），不是独立的 NPC/Bot 框架，也不能控制真实玩家
- **纯服务端**：所有行为在服务器端通过 Minecraft 原生玩家逻辑执行，客户端不需要安装本 Mod，也不需要任何客户端 Mod
- **服务器必须安装 Fabric + Carpet Mod**，本 Mod 是 Carpet 的附属扩展

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
3. 验证：`/crpi-fakeplayer list` 应列出 10 条规则

## 支持的行为（10 种）

| 动作 | 说明 |
|---|---|
| `ATTACK` | 攻击实体（原生玩家攻击路径：伤害/击退/附魔/冷却） |
| `DROP_ITEM` | 丢物品（主手/副手，单个/整组） |
| `CLOSE_GUI` | 关闭打开的容器（完整 onClosed 生命周期） |
| `USE_ITEM` | 使用手中物品 |
| `DIG` | 挖掘方块（原生挖掘公式：硬度/工具/附魔/水罚） |
| `USE` | 右键方块（放置/开箱/按钮/拉杆/门/工作台/Mod 方块） |
| `INTERACT_ENTITY` | 与实体交互（村民交易/骑乘/喂食） |
| `GUI_CLICK` | 容器槽位点击（原生 ScreenHandler.onSlotClick） |
| `USE_RELEASE` | 长按物品后释放（弓拉满射击/进食/举盾） |
| `CONTAINER_SCAN` | 扫描范围内容器（只读：不开箱、不加载区块） |

## 命令

所有命令挂载在 `/crpi fp` 下（与 CRPI Carpet 共存时自动挂到其 `crpi` 根；单独使用时自动创建根），要求 OP 权限（level 2）：

```
/crpi fp list | info
/crpi fp attack | drop | close | useitem | interact
/crpi fp dig | use
/crpi fp gui info | list | click | close
/crpi fp userelease
/crpi fp scancontainers
```

## Carpet 规则（10 条）

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

规则运行时可通过 `/crpi-fakeplayer <规则> <值>` 修改。

## Action API

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

FakePlayerActions.of(bot).attack(zombie).execute();
FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();
FakePlayerActions.of(bot).use(pos, Direction.UP).execute();
FakePlayerActions.of(bot).useRelease(Hand.MAIN_HAND, 20).execute();
FakePlayerActions.of(bot).clickSlot(0, 0, SlotActionType.PICKUP).execute();
List<ContainerScanResult> found = FakePlayerActions.of(bot).scanContainers(16);
```

所有行为走 Minecraft 原生玩家入口：`ServerPlayerEntity.attack` / `dropItem` / `closeHandledScreen`、`ServerPlayerInteractionManager`、`ScreenHandler.onSlotClick`、`onStoppedUsing` / `finishUsing`——伤害、附魔、冷却、挖掘公式、物品逻辑全部原生，零 Mixin、零新线程。

## 架构

```
Command (/crpi fp)
   ↓
FakePlayerActions（流畅 API）
   ↓
ActionDispatcher → ActionScheduler（每 server tick 驱动）
   ↓
ActionExecutor → FakePlayerHandle → ServerPlayerEntity 原生 API
```

## 已知限制

- **假人 tick 不推进持续行为**（1.21.11 多 mod 环境实测）：DIG 与 USE_RELEASE 因此由 Scheduler 自行驱动，但挖掘公式与释放逻辑仍全部走原生 API（`calcBlockBreakingDelta` / `finishMining` / `onStoppedUsing` / `finishUsing`）
- `ActionSequence` 链式队列 API 未实现（队列仅支持基础延迟/顺序）
- 容器操作需先通过 USE 打开容器；容器扫描的 `canOpen` 不模拟视线遮挡

## 文档

- [功能文档](docs/crpi-Fakeplayer功能文档.md)（命令详解、工作流、架构、限制）
- [API 文档](docs/API.md)（面向开发者的调用指南）

## 构建

需要 JDK 21：

```
gradlew.bat build
```

产物输出至 `build/libs/`。

## 许可证

[MIT](LICENSE)

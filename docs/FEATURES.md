# CRPI-FakePlayer 功能速览

> Carpet 假人的服务端行为框架（Fabric + Carpet 附属 Mod）。**纯服务端、客户端零安装、零 Mixin、零新线程**，所有行为走 Minecraft 原生玩家入口。当前版本 **0.3.0**（MC 1.21.11 / Carpet 1.4.194 / Java 21）。
>
> 详细文档：[功能文档](crpi-Fakeplayer功能文档.md) · [API 文档](API.md)

## 它能做什么（三大能力面）

### 1. Action 动作 —— 接近真实玩家的即时/持续行为
| 动作 | 说明 |
|---|---|
| `ATTACK` | 攻击实体（原版伤害/击退/附魔/冷却） |
| `DROP_ITEM` | 丢物品（主手/副手，单个/整组） |
| `CLOSE_GUI` | 关闭容器（完整 onClosed 生命周期） |
| `USE_ITEM` | 使用手中物品 |
| `DIG` | 挖掘（原版硬度/工具/附魔/水罚公式） |
| `USE` | 右键方块（放置/开箱/按钮/拉杆/门/工作台/Mod 方块） |
| `INTERACT_ENTITY` | 实体交互（村民交易/骑乘/喂食） |
| `GUI_CLICK` | 容器槽位点击（原版 `ScreenHandler.onSlotClick`） |
| `USE_RELEASE` | 长按后释放（弓满力/进食/举盾） |
| `CONTAINER_SCAN` | 只读扫描范围内容器（不开箱、不加载区块） |

所有动作由**同一个 `ActionType → Executor` 分发器 + 每 server tick 的调度器**驱动，含义统一、日志统一、可开关。

### 2. Control 控制 —— 移动/视角/状态/背包/骑乘/命令
- 移动：`moveTo` / `moveToPath`（每 tick 步进、碰撞/卡住/超时检测）
- 视角：`lookAt`（平滑转向）
- 状态：潜行、冲刺、跳跃、传送（安全落点校验）、GameMode、生命/饥饿/经验
- 背包：取出手持快照、交换主副手、`giveItem`（原生堆叠不覆盖）
- 骑乘：`mount` / `dismount`
- 命令/表现：以假人身份执行命令、发聊天、播声音

### 3. Navigation 导航 —— A* 寻路 + 物理原生移动（0.3.0）
- **物理原生**：A* 规划路线，执行层用 Carpet action pack 驱动假人输入（`setForward` 等），行走/跳跃/下落/碰撞全由原版物理完成，**从不瞬移**
- **8 种移动**：走 / 上 1 格台阶 / 下台阶 / 斜向（防斜穿墙角）/ 2-3 格安全下落 / 跳跨 1-2 格缺口 / 挖软方块开路 / 填坑过沟
- **目标系统**：`GoalBlock` / `GoalNear` / `GoalComposite` / `GoalFollow`（持续跟随，目标走远自动追击）
- **自愈**：卡住检测（40 tick）→ 自动重规划（≤3 次）；路径失效（世界变化）→ 自动重规划并惩罚当前位置换路线
- **预算同步 A\***：节点上限 1000 / 搜索半径 128 / 20ms 预算，只规划已加载区块

## 最常用命令（`/crpi fp`，OP ≥ 2）

```
/crpi fp list | info <玩家>
/crpi fp attack <玩家> <实体> | drop <玩家> [off|all] | close <玩家> | useitem <玩家>
/crpi fp dig <玩家> <坐标> [面] | use <玩家> <坐标> [面]
/crpi fp gui info|list|click|close <玩家>
/crpi fp userelease <玩家> [tick]
/crpi fp scancontainers <玩家> <半径>
/crpi fp move|lookat|jump|teleport|sneak|sprint|swap|exec <玩家> ...
/crpi fp goto <玩家> <坐标> [near <半径>] | gotoany | follow | followpath
/crpi fp navstop <玩家> | navstatus <玩家>
```

## 三种调用入口

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

bot.control().moveTo(pos, 4.3);               // Control：移动/视角/状态...
bot.navigation().gotoBlock(pos);              // Navigation：A* 寻路
FakePlayerActions.of(bot).attack(zombie).execute(); // Action：即时/持续动作
```

## 核心设计原则

- **零 Mixin**：只调用原生入口（`ServerPlayerEntity.attack/dropItem`、`ServerPlayerInteractionManager`、`ScreenHandler.onSlotClick`、`onStoppedUsing/finishUsing`），不重写游戏逻辑
- **零新线程**：所有持续行为由 Carpet `onTick` 每 tick 驱动（Action / Control / Navigation 三套 tick 链）
- **统一句柄**：执行层只通过 `FakePlayerHandle` 操作世界，假人来源可替换
- **门面 vs 直构**：`FakePlayerActions` 提供大多数便利方法；`USE_RELEASE` / `GUI_CLICK` 需直接构造 `UseReleaseAction` / `GuiClickAction` 交给 `scheduler().runNow(...)`

## 已知问题速览（详见功能文档 §7）

- ✅ 已修复：假人下线后队列/句柄/MiningSession/Control/Navigation 的统一清理；`DIG` 挖完不再残留 session
- ✅ 已修复：`maxConcurrentActions` 已强制（新结果 `CONCURRENCY_LIMIT`），`runningCount` 实时统计
- ✅ 已修复：导航中止释放进行中 `MiningSession`（不再卡死 interaction manager）；不可挖方块不再无限重挖；堆 fCost 重开更新
- ✅ 已修复：`fakePlayerActions`（总开关）与 `fakePlayerCombat`（ATTACK 开关）真正生效；`/crpi fp dig|use <...> <非法面>` 报错而非静默转 UP；非法扫描半径返回空
- 🟠 `FakePlayerActions` 缺 `useRelease()/clickSlot()`（示例需直接构造注入 scheduler）
- 🟡 导航：Break 误判一次重规划、`ALL_OF` 几乎不可用
# CRPI-FakePlayer 功能文档

> 相关文档：[API 文档](API.md)（开发者调用指南） · [README](../README.md)（项目概览）

> Carpt Fake Player 的服务端行为框架（Fabric + Carpet 附属 Mod）。通过统一的 Action API 驱动假人执行接近真实玩家的 Minecraft 行为。
> **纯服务端，客户端无需安装，零 Mixin，零新线程。** 目标 Minecraft **1.21.11** / Carpet **1.4.194**。

---

## 1. 安装与启用

1. 将 `crpi-fakeplayer-0.3.0.jar` 放入服务器 `mods/` 目录
2. 客户端**不需要**安装任何东西
3. 启动后输入 `/crpi-fakeplayer list`，能列出 9 条规则即加载成功
4. 假人由 Carpet 生成：`/player <名字> spawn`
5. 扩展规则通过 `/crpi-fakeplayer <规则名> <值>` 设置

---

## 2. Carpet 规则一览（9 条）

| 规则 | 默认 | 说明 |
|---|---|---|
| `fakePlayerActions` | `true` | 总开关：关闭后 `/crpi fp` 命令与整个 Action 框架停用 |
| `fakePlayerCombat` | `true` | ATTACK 动作开关 |
| `fakePlayerItemUse` | `true` | USE_ITEM / USE_RELEASE 动作开关 |
| `fakePlayerInteraction` | `true` | USE / INTERACT_ENTITY 动作开关 |
| `fakePlayerMining` | `true` | DIG 动作开关 |
| `fakePlayerContainer` | `true` | GUI_CLICK 等容器操作开关 |
| `fakePlayerDebug` | `false` | 开启后输出每个 Action 的详细日志（玩家/动作/状态/结果） |
| `maxQueueLength` | `64` | 每个假人的 Action 队列最大长度 |
| `maxConcurrentActions` | `16` | 同时运行中的 Stateful Action 数量上限 |
| `maxContainerScanRadius` | `16` | 容器扫描命令允许的最大半径（防卡顿） |

---

## 3. 命令参考

所有命令挂载在 **`/crpi fp`** 下（与 CRPI Carpet 共存时自动挂到其 `crpi` 根；单独使用时自动创建根）。要求 OP 权限（level 2）。

### 3.1 基础

```
/crpi fp list                      在线假人列表
/crpi fp info <玩家>               假人状态（位置/维度/游戏模式/当前容器）
```

### 3.2 即时动作

```
/crpi fp attack <玩家> <实体>      攻击实体（原版玩家攻击路径：伤害/击退/附魔/冷却）
/crpi fp drop <玩家> [off|all]     丢物品（主手/副手；all = 整组）
/crpi fp close <玩家>              关闭当前打开的容器（走完整 onClosed 生命周期）
/crpi fp useitem <玩家> [off]      使用手中物品（物品自行决定行为）
/crpi fp interact <玩家> <实体> [off]  与实体交互（村民交易/骑乘/喂食）
```

### 3.3 方块交互

```
/crpi fp dig <玩家> <坐标> [面]    挖掘方块（状态机 RUNNING→SUCCESS）
/crpi fp use <玩家> <坐标> [面]    右键方块（放置/开箱/按钮/拉杆/门/工作台/Mod 方块）
```

`面` 可选：`up/down/north/south/east/west`（默认 `up`）。
挖掘执行前自动校验：chunk 已加载、距离 ≤6、方块可挖；不可挖方块（基岩等）快速失败。

### 3.4 容器操作

```
/crpi fp gui info <玩家>            当前容器信息（类型/syncId/槽位数）
/crpi fp gui list <玩家>            容器内容列表
/crpi fp gui click <玩家> <槽位> <按钮> <操作>   槽位点击
/crpi fp gui close <玩家>           关闭容器
```

`操作` 可选（对应原版 `SlotActionType`）：

| 操作 | 说明 | 按钮约束 |
|---|---|---|
| `pickup` | 拿起/放置（含合并与交换） | 0 |
| `quick_move` | 快速移动（Shift 点击） | 0 |
| `swap` | 与热键栏交换 | 0-8 |
| `clone` | 创造模式复制 | 0 |
| `throw` | 丢出槽位物品 | 0 |
| `pickup_all` | 拾取同类物品 | 0 |

执行前校验：容器已打开、handler 可用、槽位合法、按钮合法；错误返回 `INVALID_STATE` / `INVALID_TARGET`，不会静默失败。

### 3.5 容器扫描

```
/crpi fp scancontainers <玩家> <半径>   扫描以假人为中心的立方体范围内容器（只读）
```

输出示例：

```
Found 3 containers.
[1] 100 64 200 type=minecraft:chest canOpen=true items:
  minecraft:diamond x8
  minecraft:iron_ingot x32
```

特性：

- **只读**：不打开 GUI、不修改容器、不主动加载区块（只扫已加载区块）
- 通过原生 `Inventory` 接口识别容器，Mod 容器自动兼容
- 相同物品自动聚合数量；空槽位与空容器不输出物品行
- `canOpen` = 实体存活 + 原版交互距离内 + 无锁（`LootableContainerBlockEntity.checkUnlocked`）
- 双箱只报告主箱一次（副箱跳过）
- 半径上限 `maxContainerScanRadius`（默认 16），超出报 `Invalid radius`

### 3.6 长按物品使用（Stateful）

```
/crpi fp userelease <玩家> [off] [tick]   长按物品 tick 后释放（默认 20 tick = 1 秒）
```

| 物品类型 | 行为 |
|---|---|
| 弓 / 弩 / 三叉戟 | 按 tick 计算力度发射（20 tick = 满力） |
| 盾 | 举盾 tick 后放下 |
| 食物 / 药水 | 直接完成进食/饮用 |
| 望远镜 | 停止使用 |

---

### 3.7 移动 / 视角 / 状态（0.2.0 Control 命令）

```
/crpi fp move <玩家> <坐标> [速度]        移动到目标（速度默认 4.3 格/秒）
/crpi fp lookat <玩家> <坐标>             平滑转向目标（2 tick）
/crpi fp jump <玩家>                      跳跃
/crpi fp teleport <玩家> <坐标>           安全传送（校验落点：脚下有方块、上下无遮挡）
/crpi fp sneak <玩家> [off]               潜行开关
/crpi fp sprint <玩家> [off]              冲刺开关
/crpi fp swap <玩家>                      交换主副手
/crpi fp exec <玩家> <命令>               以假人身份执行命令
```

移动特性：每 tick 步进（无阻塞、无新线程）、碰撞检测（障碍 → FAIL）、卡住检测（20 tick 无位移 → FAIL）、超时 600 tick；`moveTo` 返回 `PASS`（进行中）→ `SUCCESS`（到达，误差 0.5 格）。

### 3.8 导航（0.3.0 Navigation 命令）

```
/crpi fp goto <玩家> <坐标> [near <半径>]   A* 寻路前往（near = 到达半径内即可）
/crpi fp gotoany <玩家> <坐标1> <坐标2>    任一目标可达即成功
/crpi fp follow <玩家> <实体>              持续跟随实体（默认 2 格，目标走远自动追击）
/crpi fp followpath <玩家> <x,z> [<x,z>...] 显式路径点执行（同高直线）
/crpi fp navstop <玩家>                    停止导航
/crpi fp navstatus <玩家>                  导航状态查询
```

导航特性：

- **物理原生**：A* 规划路线，执行层通过 Carpet action pack 驱动假人——行走/跳跃/下落/碰撞全部原版物理，**从不瞬移**
- **8 种移动**：平地走、上 1 格台阶（跳）、下台阶、斜向（防斜穿墙角）、2-3 格安全下落、疾跑跳跨 1-2 格缺口、挖掘软方块开路、放置方块填坑过沟
- **预算同步 A***：节点上限 1000 / 搜索半径 128 / 20ms 预算，永不阻塞服务器；只规划已加载区块
- **自愈**：卡住检测（40 tick）→ 自动重规划（≤3 次）；路径失效（世界变化）→ 自动重规划
- `navstatus` 输出：`status=SUCCESS repaths=0 goal=GoalBlock{0,89,8}`（状态：IDLE/CALCULATING/RUNNING/SUCCESS/FAILED/STUCK/CANCELLED）

## 4. Action API（供其它 Mod 调用）

```java
import com.crpi.fakeplayer.api.FakePlayerActions;
import com.crpi.fakeplayer.fakeplayer.FakePlayerAdapter;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.action.ActionResult;

MinecraftServer server = ...;
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

// 攻击
ActionResult r1 = FakePlayerActions.of(bot).attack(zombie).execute();

// 挖掘（原生挖掘公式，空手/工具/附魔全部生效）
ActionResult r2 = FakePlayerActions.of(bot).dig(pos, Direction.UP).execute();

// 右键方块（开箱/放置/按钮）
ActionResult r3 = FakePlayerActions.of(bot).use(pos, Direction.UP).execute();

// 使用物品 / 长按释放（弓 20 tick 满力）
ActionResult r4 = FakePlayerActions.of(bot).useItem(Hand.MAIN_HAND).execute();
ActionResult r5 = FakePlayerActions.of(bot).useRelease(Hand.MAIN_HAND, 20).execute();

// 与实体交互
ActionResult r6 = FakePlayerActions.of(bot).interact(villager, Hand.MAIN_HAND).execute();

// 容器槽位点击（PICKUP slot 0 → 玩家背包）
ActionResult r7 = FakePlayerActions.of(bot).clickSlot(0, 0, SlotActionType.PICKUP).execute();

// 关容器 / 丢物品
ActionResult r8 = FakePlayerActions.of(bot).closeGui().execute();
ActionResult r9 = FakePlayerActions.of(bot).drop(Hand.MAIN_HAND, false).execute();
```

`ActionResult` 枚举：`SUCCESS / PASS / FAIL / RETRY / SKIP / ABORT / INVALID_TARGET / OUT_OF_RANGE / NO_PERMISSION / INVALID_STATE`。
Stateful 动作（DIG / USE_RELEASE）`execute()` 返回 `PASS` 表示已开始（状态 `RUNNING`），完成后由框架置为 `SUCCESS/FAILED`。

### 4.1 Control API（0.2.0，完整签名）

```java
import com.crpi.fakeplayer.control.FakePlayerControl;
import com.crpi.fakeplayer.control.ContainerInfo;
import com.crpi.fakeplayer.control.ItemStackSnapshot;

FakePlayerControl ctl = bot.control();

// 移动 / 视角（持续任务，tick 驱动，PASS=进行中 SUCCESS=完成 FAIL=失败）
ActionResult r1 = ctl.moveTo(pos, 4.3);
ActionResult r2 = ctl.moveToPath(List.of(p1, p2, p3), 4.3);
ActionResult r3 = ctl.lookAt(pos);          // 或 lookAt(entity)
ActionResult r4 = ctl.pathfindTo(pos, 4.3); // 直线版，寻路 TODO

// 状态
ActionResult r5 = ctl.sneak(true);
ActionResult r6 = ctl.sprint(true);
ActionResult r7 = ctl.jump();
ActionResult r8 = ctl.teleportTo(pos);      // 安全落点校验
ActionResult r9 = ctl.setGameMode(GameMode.SURVIVAL);
ActionResult r10 = ctl.setHealth(20.0);    // 按最大生命值校验
ActionResult r11 = ctl.setFoodLevel(20);
ActionResult r12 = ctl.addExperience(10);

// 背包 / 物品（快照不可变；giveItem 原生堆叠不覆盖）
ItemStackSnapshot snap = ctl.getHeldItem(Hand.MAIN_HAND);
ActionResult r13 = ctl.swapHands();
ActionResult r14 = ctl.setHeldItem(Hand.MAIN_HAND, stack);
ActionResult r15 = ctl.giveItem(stack);

// 骑乘 / 交互
ActionResult r16 = ctl.mount(vehicle);
ActionResult r17 = ctl.dismount();
ActionResult r18 = ctl.interactBlock(pos, Direction.UP, Hand.MAIN_HAND);

// 命令 / 表现（以假人身份）
ActionResult r19 = ctl.executeCommand("say hello");
ActionResult r20 = ctl.sendChatMessage("hi");
ActionResult r21 = ctl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);

// 环境感知（只读）
ContainerInfo info = ctl.getContainerInfo();
List<ContainerInfo> nearby = ctl.getNearbyContainers(8);
```

`ContainerInfo`：`pos / blockId / canOpen / items(List<ItemStackSnapshot>)`；`ItemStackSnapshot`：`itemId(Identifier) / count(int)`。

### 4.3 Navigation API（0.3.0，完整签名）

```java
import com.crpi.fakeplayer.navigation.NavigationManager;
import com.crpi.fakeplayer.navigation.NavigationStatus;
import com.crpi.fakeplayer.navigation.goal.*;
import com.crpi.fakeplayer.navigation.cost.*;

NavigationManager nav = bot.navigation();   // 每假人单例，每 tick 由 NavigationRegistry 驱动

// 目标
boolean ok1 = nav.gotoBlock(pos);               // A* 到精确方块
boolean ok2 = nav.gotoNear(pos, 3);             // 半径内
boolean ok3 = nav.gotoAny(posA, posB);          // 任一可达
nav.follow(entity);                             // 持续跟随（目标走远自动恢复追击）
nav.follow(entity, 3);
boolean ok4 = nav.followPath(List.of(p1, p2));  // 显式路径点（同高直线）

// 控制
nav.stop(); nav.pause(); nav.resume(); nav.repath();
NavigationStatus s = nav.status();              // IDLE/CALCULATING/RUNNING/SUCCESS/FAILED/STUCK/CANCELLED
boolean busy = nav.isNavigating();
Goal g = nav.goal();
Path path = nav.currentPath();
int repaths = nav.repaths();

// 配置
nav.profile().allowBreak = ...;                 // 挖软方块开路（默认 true）
nav.profile().allowPlace = ...;                 // 填坑过沟（默认 true）
nav.costModel().setBlockCost(Blocks.LAVA, 100000.0);  // 危险方块代价
nav.favoring().favor(pos, 500.0);               // 失败位置惩罚（重规划换路线）
```

移动类型：`Traverse`（走）/ `Ascend`（上台阶）/ `Descend`（下台阶）/ `Diagonal`（斜向）/ `Fall`（2-3 格安全下落）/ `Parkour`（疾跑跳 1-2 格缺口）/ `Break`（挖软方块）/ `Place`（填坑）。

引擎：预算同步 A*（1000 节点/128 半径/20ms，超限返回 PARTIAL 局部最优路径）；卡住检测 40 tick + 自动重规划 ≤3 次（跟随任务不占配额）；动态路径失效检测（目标不可站 → 重规划）；CostModel 默认熔岩 10 万/火 1 千/仙人掌 500 + 3×3 危险环惩罚；只规划已加载区块。

### 4.2 控制任务机制

- 移动/视角任务由 `ControlManager` 每 tick 驱动（Carpet `onTick`），无阻塞、无新线程
- 同一假人同时只有一个控制任务；新任务自动取消旧任务；`teleportTo` 会清空移动任务
- 移动：每 tick 位置步进 + `blocksMovement()` 碰撞检测（障碍 FAIL）+ 卡住检测（20 tick）+ 超时（600 tick）
- 说明：Carpet 假人不按 velocity 移动（1.21.11 实测），因此采用每 tick 位置步进而非原版速度机制；碰撞判定仍走原版 `blocksMovement()`

---

## 5. 典型工作流

### 5.1 采集方块

```
/player bot1 spawn
/crpi fp dig bot1 100 64 200 down        # RUNNING（渐进挖掘）
/crpi fp dig bot1 100 64 200 down        # 完成后方块消失
```

### 5.2 开箱取物

```
/crpi fp use bot1 101 64 200 east        # 开箱
/crpi fp gui list bot1                   # 查看内容
/crpi fp gui click bot1 0 0 pickup       # 拿起第 0 格
/crpi fp gui click bot1 36 0 pickup      # 放入玩家背包
/crpi fp gui click bot1 36 0 quick_move  # 快速移回箱子
/crpi fp gui close bot1                  # 关箱
```

### 5.3 战斗演示

```
/crpi fp attack bot1 @e[type=minecraft:zombie,sort=nearest,limit=1]
```

### 5.4 远程武器

```
/player bot1 look south
/crpi fp userelease bot1 20              # 拉弓 1 秒满力射出
```

### 5.5 导航演示

```
/crpi fp goto bot1 100 64 200            # A* 寻路（自动绕障碍/上台阶/跳缺口）
/crpi fp goto bot1 100 64 200 near 3     # 到达 3 格半径内即可
/crpi fp navstatus bot1                  # status=SUCCESS repaths=0
/crpi fp follow bot1 bot2                # 持续跟随 bot2
/crpi fp followpath bot1 5,0 5,5 0,5     # 显式 L 形路径
/crpi fp navstop bot1                    # 停止
```

---

## 6. 架构

```
Action（参数 + 状态 + 结果）
    ↓
ActionDispatcher（type → executor 注册表）
    ↓
ActionScheduler（每 server tick 由 Carpet onTick 驱动，无额外线程）
    ↓
ActionExecutor → FakePlayerHandle → ServerPlayerEntity 原生 API
```

```
com.crpi.fakeplayer/
├── action/          Action 抽象（id/type/状态/结果）、ActionType、ActionResult、
│   │                ActionState、ActionExecutor 接口、ActionDispatcher
│   ├── impl/        9 种动作参数类
│   └── executor/    9 种动作执行器 + Executors 注册
├── fakeplayer/      Carpet 适配层：FakePlayerAdapter（EntityPlayerMPFake 识别与解析）、
│                    FakePlayerHandle（对外句柄）、FakePlayerRegistry（缓存）
├── scheduler/       ActionScheduler（tick 驱动）+ ActionQueue（每假人队列）
├── mining/          MiningSession（挖掘会话状态机）+ MiningManager
├── itemuse/         ItemUseSession（长按使用会话状态机）
├── container/       ContainerContext（容器快照与校验）+ ContainerManager
├── control/         Control API：FakePlayerControl（24 方法）+ MoveTask/LookTask + ControlManager
├── navigation/      Navigation API：NavigationManager（目标/状态/重规划）+ NavigationRegistry（tick 驱动）
│   ├── goal/        GoalBlock / GoalNear / GoalComposite / GoalFollow
│   ├── path/        PathNode（安全哈希）+ Path
│   ├── pathfinding/ AStarPathFinder（预算控制）+ BinaryHeapOpenSet + PathCalculationResult
│   ├── movement/    Movement 接口 + 8 种实现（Traverse/Ascend/Descend/Diagonal/Fall/Parkour/Break/Place）
│   │   └── controller/  FakePlayerMovementController（Carpet action pack 输入桥）
│   ├── cost/        CostModel（方块代价）+ Favoring（位置惩罚）
│   ├── world/       NavigationWorld（已加载区块约束）+ StandPositionFinder
│   └── executor/    PathExecutor（卡住检测/路径失效检测）
├── command/         /crpi fp 调试命令（薄壳，只调 Action API）
├── config/          Carpet 规则
└── api/             FakePlayerActions 流畅 API
```

**设计原则**：
- 所有行为走 Minecraft 原生玩家入口（`ServerPlayerEntity.attack/dropItem/closeHandledScreen`、`ServerPlayerInteractionManager`、`ScreenHandler.onSlotClick`、`onStoppedUsing/finishUsing`），不重复实现挖掘公式、伤害计算、物品逻辑
- 导航移动走 Carpet action pack（`getActionPack().setForward` 等输入），跳跃/下落/碰撞全部原版物理，不瞬移
- Action Executor 不直接接触 Carpet 内部实现，只通过 `FakePlayerHandle`
- 命令只是调试薄壳：Command → Action API → Dispatcher → Executor

---

## 7. 已知限制与说明

1. **假人 tick 不推进持续行为**：在 1.21.11 多 mod 环境下，Carpet 假人的 tick 不会可靠推进 `interactionManager.update()`（挖掘）与物品使用计时（连 Carpet 自带的 `use continuous` 也无法拉弓释放）。因此：
   - DIG：由 Scheduler 每 tick 累积原生 `calcBlockBreakingDelta`，达到 1.0 后调原生 `finishMining` 破坏——挖掘公式 100% 原生
   - USE_RELEASE：由 Scheduler 计时，释放时直接调用物品原生的 `onStoppedUsing`（弓按力度发射）或 `finishUsing`（食物完成）
2. **ActionSequence 链式队列**未实现（队列仅支持基础延迟/顺序）
3. 容器操作需先通过 USE 打开容器；`GUI_CLICK` 仅支持使用标准 `ScreenHandler` 的容器（Mod 容器一般自动兼容）
4. 虚空世界假人会坠落死亡（测试请用普通/超平坦世界）
5. 与 CRPI Carpet 共存时命令自动挂载到同一 `crpi` 根；两者规则互不干扰
6. **导航限制**（0.3.0）：不支持游泳/水路；挖掘只限软方块（硬度 ≤1.5，硬方块需工具属后续版本）；`followPath` 仅同高直线 waypoints；A* 为同步计算（20ms 预算兜底，超限返回局部最优路径继续执行）；只规划已加载区块（不主动加载）

---

## 8. 版本路线

| 版本 | 内容 | 状态 |
|---|---|---|
| V0.1 | 框架 + ATTACK / DROP_ITEM / CLOSE_GUI / USE_ITEM | ✅ 已实测 |
| V0.2 | DIG（MiningSession）/ USE / INTERACT_ENTITY | ✅ 已实测 |
| V0.3 | 容器操作：GUI_CLICK / ContainerContext / ContainerManager | ✅ 已实测 |
| V0.4 | Stateful：USE_RELEASE / ItemUseSession | ✅ 已实测 |
| 0.2.0 | Control API：移动 / 视角 / 状态 / 背包 / 骑乘 / 命令 / 环境感知 | ✅ 已实测 |
| 0.3.0 | Navigation 系统：A* 寻路 + 8 种物理原生移动 + 目标系统 + 自愈执行器 | ✅ 已实测 |
| 未来 | ActionSequence、游泳/水路寻路、Replay/TAS 桥接、AI、Litematica Bridge | 规划中 |

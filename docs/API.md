# CRPI-FakePlayer API 文档

> 面向在其它 Mod 中调用 CRPI-FakePlayer 行为能力的开发者。目标 MC 1.21.11 / Carpet 1.4.194，纯服务端。

## 1. 快速开始

### 依赖（build.gradle）

```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    // 与目标服务器一致的版本
    modImplementation "net.fabricmc.fabric-loader:${project.loader_version}"
    modImplementation "maven.modrinth:carpet:${project.carpet_version}"
    // 本地依赖：将 crpi-fakeplayer jar 加入 libs 或使用本地 maven
    modImplementation files("libs/crpi-fakeplayer-0.3.0.jar")
}
```

### 获取假人句柄

```java
import com.crpi.fakeplayer.fakeplayer.FakePlayerAdapter;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;

MinecraftServer server = ...; // 从你的入口获取
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");
if (bot == null) {
    // 假人不在线，或该名字是真实玩家
}
```

## 2. 核心类型

### ActionType

```java
public enum ActionType {
    ATTACK, DROP_ITEM, CLOSE_GUI, USE_ITEM,
    DIG, USE, INTERACT_ENTITY,
    GUI_CLICK, USE_RELEASE
    // CONTAINER_SCAN 不在 Action 系统内（同步工具，见 §5）
}
```

### ActionResult

```java
public enum ActionResult {
    SUCCESS, PASS, FAIL, RETRY, SKIP, ABORT,
    INVALID_TARGET, OUT_OF_RANGE, NO_PERMISSION, INVALID_STATE
}
```

- 瞬时动作：`execute()` 返回 `SUCCESS`/`FAIL`/`INVALID_*` 等终态
- 持续动作（DIG / USE_RELEASE）：返回 `PASS` 表示已开始（`RUNNING`），完成后框架置为 `SUCCESS`/`FAILED`

### ActionState

```java
CREATED → QUEUED → STARTED → RUNNING → SUCCESS | FAILED | CANCELLED
```

### Action（基类）

```java
public abstract class Action {
    public long id();
    public ActionType type();
    public FakePlayerHandle handle();
    public long createdTick();
    public long scheduledTick();
    public ActionState state();
    public ActionResult result();
}
```

## 3. FakePlayerActions（流畅 API）

```java
import com.crpi.fakeplayer.api.FakePlayerActions;
import com.crpi.fakeplayer.action.ActionResult;

FakePlayerActions api = FakePlayerActions.of(bot);
```

| 方法 | 返回 | 说明 |
|---|---|---|
| `attack(Entity target)` | `AttackAction` | 攻击实体（目标需同世界、存活、≤4 格） |
| `drop()` / `drop(Hand, boolean entireStack)` | `DropItemAction` | 丢物品（主手/副手，单/组） |
| `closeGui()` | `CloseGuiAction` | 关闭当前容器（完整 onClosed 生命周期） |
| `useItem(Hand)` | `UseItemAction` | 使用手中物品 |
| `dig(BlockPos, Direction)` | `DigAction` | 挖掘（持续，原生挖掘公式） |
| `use(BlockPos, Direction)` | `UseAction` | 右键方块（放置/开箱/按钮/门） |
| `interact(Entity, Hand)` | `InteractEntityAction` | 与实体交互（村民/骑乘/喂食） |
| `clickSlot(int slot, int button, SlotActionType)` | `GuiClickAction` | 容器槽位点击（需已打开容器） |
| `useRelease(Hand, long ticks)` | `UseReleaseAction` | 长按物品 tick 后释放（弓 20=满力） |
| `scanContainers(int radius)` | `List<ContainerScanResult>` | 同步扫描范围内容器（只读） |
| `execute(Action)` | `ActionResult` | 立即执行一个动作 |

完整示例：

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

// 瞬时动作：立即得到终态结果
ActionResult r1 = FakePlayerActions.of(bot).attack(zombie).execute();

// 持续动作：PASS = 已开始（RUNNING），可轮询 action.state()
DigAction dig = FakePlayerActions.of(bot).dig(pos, Direction.UP);
ActionResult r2 = dig.execute();
if (r2 == ActionResult.PASS) {
    while (!dig.state().isTerminal()) {
        // 等待若干 tick（或由你的调度驱动）
    }
    System.out.println("result: " + dig.result());
}

// 容器：USE 开箱 → 点击 → 关箱
FakePlayerActions.of(bot).use(chestPos, Direction.UP).execute();
FakePlayerActions.of(bot).clickSlot(0, 0, SlotActionType.PICKUP).execute();
FakePlayerActions.of(bot).closeGui().execute();

// 扫描：同步返回，无需轮询
List<ContainerScanResult> found = FakePlayerActions.of(bot).scanContainers(16);
```

## 4. Action 调度

- 所有动作统一由 `CRPIFakePlayerMod.scheduler()`（`ActionScheduler`）驱动
- **服务器主线程执行，每 tick 由 Carpet onTick 推进**，无额外线程
- 每个假人有独立 `ActionQueue`：支持延迟（`scheduledTick`）与顺序执行
- 安全上限：`maxQueueLength`（64）、`maxConcurrentActions`（16）、动作链深度 8

```java
// 队列延迟执行（scheduler 层 API）
ActionScheduler scheduler = CRPIFakePlayerMod.scheduler();
ActionQueue queue = scheduler.queueOf(bot);
queue.schedule(FakePlayerActions.of(bot).attack(target), 20); // 20 tick 后执行
```

## 5. 容器扫描（同步 API）

```java
import com.crpi.fakeplayer.container.ContainerScanner;
import com.crpi.fakeplayer.container.ContainerScanResult;
import com.crpi.fakeplayer.container.ItemStackInfo;

List<ContainerScanResult> results = ContainerScanner.scan(bot, 16);
for (ContainerScanResult r : results) {
    r.pos();      // BlockPos
    r.blockId();  // Identifier，如 minecraft:chest
    r.canOpen();  // 实体存活 + 交互距离内 + 无锁
    r.items();    // List<ItemStackInfo>（同物品已聚合）
}
```

`ItemStackInfo`：`itemId()`（Identifier）+ `count()`。
扫描为**只读**：不开箱、不加载区块、不修改世界。半径自动钳制到 `maxContainerScanRadius`。

## 6. Control API（0.2.0 新增）

完整签名与语义（挂在 `FakePlayerHandle.control()` 或 `FakePlayerActions.of(bot)` 上）：

| 方法 | 返回 | 说明 |
|---|---|---|
| `moveTo(BlockPos, double speed)` | `ActionResult` | 移动到方块；`PASS`=进行中，`SUCCESS`=到达（误差 0.5），`FAIL`=障碍/超时/卡住 |
| `moveToPath(List<BlockPos>, double)` | `ActionResult` | 按路径点顺序移动 |
| `lookAt(BlockPos)` / `lookAt(Entity)` | `ActionResult` | 2 tick 平滑转向目标中心 |
| `sneak(boolean)` / `sprint(boolean)` | `ActionResult` | 潜行/冲刺（原生状态） |
| `jump()` | `ActionResult` | 原生跳跃 |
| `teleportTo(BlockPos)` | `ActionResult` | 安全传送（落点：脚下有方块、上下无遮挡；清空移动任务；先下坐骑） |
| `getHeldItem(Hand)` | `ItemStackSnapshot` | 不可变快照（itemId + count） |
| `swapHands()` | `ActionResult` | 主副手交换 |
| `setHeldItem(Hand, ItemStack)` | `ActionResult` | 设置手部物品（copy 隔离） |
| `interactBlock(BlockPos, Direction, Hand)` | `ActionResult` | 全参数方块交互（距离校验 + 原生 interactBlock） |
| `mount(Entity)` / `dismount()` | `ActionResult` | 骑乘 / 下坐骑（原生 startRiding/stopRiding） |
| `startRiding(Entity, boolean)` | `ActionResult` | mount 的别名 |
| `giveItem(ItemStack)` | `ActionResult` | 原生堆叠插入（优先同类堆叠→空槽，不覆盖） |
| `setHealth(double)` | `ActionResult` | 按 `getMaxHealth()` 校验 |
| `setFoodLevel(int)` | `ActionResult` | 0~20，满饥饿补满饱和 |
| `addExperience(int)` | `ActionResult` | 原生经验系统 |
| `executeCommand(String)` | `ActionResult` | **以假人身份**执行命令（`parseAndExecute` + 假人 CommandSource） |
| `playSound(SoundEvent)` | `ActionResult` | 播放声音 |
| `setGameMode(GameMode)` | `ActionResult` | 切换游戏模式 |
| `sendChatMessage(String)` | `ActionResult` | 以假人名义广播聊天 |
| `getContainerInfo()` | `ContainerInfo` | 当前打开容器快照（无容器返回 null） |
| `pathfindTo(BlockPos, double)` | `ActionResult` | **直线移动版**；真实寻路（避障）TODO |
| `getNearbyContainers(double radius)` | `List<ContainerInfo>` | 环境感知：只读扫描（复用 ContainerScanner，自动钳制 `maxContainerScanRadius`） |

`ContainerInfo`：`pos / blockId / canOpen / items(List<ItemStackSnapshot>)`。
`ItemStackSnapshot`：`itemId(Identifier) / count(int)`，不可变，修改不影响假人背包。

```java
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "builder1");

bot.control().lookAt(chestPos);
bot.control().moveTo(chestPos, 4.3);
List<ContainerInfo> nearby = bot.control().getNearbyContainers(8);
for (ContainerInfo c : nearby) {
    System.out.println(c.pos() + " " + c.blockId() + " canOpen=" + c.canOpen());
}
```

### 任务机制与限制

- 持续任务（move/look/path）由 `ControlManager` 每 tick 驱动（Carpet `onTick`）；同一假人单任务模型，新任务自动取消旧任务
- 移动为每 tick 位置步进 + 原版 `blocksMovement()` 碰撞检测（障碍 FAIL）、20 tick 卡住检测、600 tick 超时
- **已知限制**：Carpet 假人不按 velocity 移动（1.21.11 实测），移动因此不走原版速度机制；`pathfindTo` 无避障
- **注意**：Control 的 `moveTo`/`pathfindTo` 是直线工具；需要真实避障寻路请使用 §7 Navigation API

## 7. Navigation API（0.3.0 新增）

物理原生 A* 寻路：规划器计算路线，执行层通过 Carpet action pack 驱动假人——行走、跳跃、下落、碰撞全部是 Minecraft 原生物理，**从不瞬移**。

### 7.1 入口

```java
import com.crpi.fakeplayer.navigation.NavigationManager;
import net.minecraft.util.math.BlockPos;

NavigationManager nav = bot.navigation();   // 每假人单例，由 NavigationRegistry 每 tick 驱动
```

### 7.2 完整方法签名

| 方法 | 返回 | 说明 |
|---|---|---|
| `gotoBlock(BlockPos)` | `boolean` | A* 导航到方块；`true`=已开始（RUNNING 或已到达 SUCCESS），`false`=无路径（FAILED） |
| `gotoNear(BlockPos, int radius)` | `boolean` | 到达半径内即成功（GoalNear） |
| `gotoAny(BlockPos... targets)` | `boolean` | 任一目标可达即成功（GoalComposite ANY_OF） |
| `follow(Entity)` / `follow(Entity, int distance)` | `void` | **持续跟随**实体（默认 2 格）；目标走远自动恢复追击，目标死亡 → FAILED |
| `followPath(List<BlockPos>)` | `boolean` | 显式路径点执行（同高直线，无 A*） |
| `stop()` | `void` | 停止（CANCELLED）并释放输入 |
| `pause()` / `resume()` | `void` | 暂停（停止输入）/ 从当前目标重规划恢复 |
| `repath()` | `boolean` | 手动重规划（有 20 tick 冷却，超 3 次 FAILED；跟随任务不占配额） |
| `status()` | `NavigationStatus` | `IDLE / CALCULATING / RUNNING / SUCCESS / FAILED / STUCK / CANCELLED` |
| `isNavigating()` / `isFinished()` | `boolean` | 进行中 / 已终态 |
| `goal()` / `currentPath()` | `Goal` / `Path` | 当前目标 / 正在执行的路径（可能为 null） |
| `repaths()` | `int` | 已重规划次数 |
| `profile()` / `setProfile(NavigationProfile)` | — | 行为开关配置 |
| `costModel()` | `CostModel` | 方块代价模型（危险/地形惩罚） |
| `favoring()` | `Favoring` | 位置代价调整（失败位置惩罚） |

### 7.3 目标（Goal）

```java
new GoalBlock(pos);              // 精确方块
new GoalNear(pos, radius);       // 半径内
new GoalComposite(               // 组合
    GoalComposite.Mode.ANY_OF,   // 任一满足（或 ALL_OF 全部满足）
    goalA, goalB);
new GoalFollow(entity, 2);       // 跟随实体（持续）
```

### 7.4 配置

```java
NavigationProfile p = nav.profile();
p.allowBreak;          // true  — 允许挖软方块开路（硬度 ≤1.5）
p.allowPlace;          // true  — 允许放置方块填坑过沟
p.allowParkour;        // true  — 允许疾跑跳跨 1-2 格缺口
p.allowSprint;         // true
p.allowSwim;           // false — 水路未实现
p.maxFallDistance;     // 3     — 安全下落上限（格）
p.maxSearchRadius;     // 128
p.maxCalculationBudgetNanos;  // 20_000_000（20ms 预算）

nav.costModel().setBlockCost(Blocks.LAVA, 100000.0);   // 自定义危险代价
nav.favoring().favor(pos, 500.0);                      // 避开某位置
nav.favoring().clear();
```

### 7.5 支持的移动类型（8 种）

| Movement | 说明 |
|---|---|
| `MovementTraverse` | 平地 1 格走（action pack forward） |
| `MovementAscend` | 上 1 格台阶（原生跳跃） |
| `MovementDescend` | 下 1 格台阶 |
| `MovementDiagonal` | 斜向（**防斜穿墙角**：两个相邻直向位置必须可通行） |
| `MovementFall` | 2-3 格安全下落（下落通道逐层校验；超过 maxFallDistance 绝不规划） |
| `MovementParkour` | 疾跑跳跨 1-2 格缺口（缺口脚下悬空、两侧同高） |
| `MovementBreak` | 挖掘软方块开路（复用 vanilla MiningSession：原生速度/工具/掉落） |
| `MovementPlace` | 背包方块填坑后走过（setBlockState + 背包扣 1） |

### 7.6 引擎行为

- **预算同步 A***：节点上限 1000 / 搜索半径 128 / 20ms 时间预算；超出返回 PARTIAL（最优局部路径）继续执行并重规划，**永不阻塞服务器**
- **只规划已加载区块**（不主动加载区块）
- **自愈**：卡住检测（40 tick 无位移）→ 位置惩罚 + 自动重规划（≤3 次）；世界变化使当前路径失效（目标不可站）→ 自动重规划
- **代价系统**：`CostModel`（默认熔岩 10 万/火 1 千/仙人掌 500 + 周围 3×3 危险环惩罚）+ `Favoring`（失败位置 +1000/+500，重规划换路线）
- 同一假人导航与 Control 任务相互独立；建议不要同时驱动

### 7.7 示例

```java
NavigationManager nav = bot.navigation();

// 走到箱子旁边
nav.gotoBlock(new BlockPos(100, 64, 200));
while (nav.isNavigating()) { /* 等待 tick */ }
System.out.println(nav.status());           // SUCCESS

// 半径 3 内即可
nav.gotoNear(chestPos, 3);

// 跟随另一个假人，直到目标下线
nav.follow(otherPlayer, 2);

// 显式路径（同高直线 waypoints）
nav.followPath(List.of(new BlockPos(100, 64, 200), new BlockPos(105, 64, 200)));

// 查询
nav.goal(); nav.currentPath(); nav.repaths(); nav.status();
```

### 7.8 与 Control API 的区别

| | Control API（0.2.0） | Navigation API（0.3.0） |
|---|---|---|
| 定位 | 直线移动/视角/背包/命令等通用控制 | A* 寻路 + 路径执行 |
| 移动方式 | 每 tick 位置步进 + 碰撞检测 | **Carpet action pack 原生物理**（跳/落/碰撞全原生） |
| 避障 | 无（障碍直接 FAIL） | A* 自动绕行 + 挖掘/放置开路 |
| 适用 | 短距离直线、工具型操作 | 中长距离真实寻路、跟随 |

## 8. 扩展自定义 Action

```java
// 1. 定义动作
public final class MyAction extends Action {
    private final BlockPos target;
    public MyAction(FakePlayerHandle h, long tick, BlockPos target) {
        super(ActionType.USE_ITEM /* 复用类型 */, h, tick);
        this.target = target;
    }
    public BlockPos target() { return this.target; }
}

// 2. 实现执行器
public final class MyExecutor implements ActionExecutor<MyAction> {
    @Override
    public ActionResult execute(MyAction action, FakePlayerHandle handle) {
        // 瞬时：直接执行并返回终态
        // 持续：返回 ActionResult.RETRY，并在 tick() 里推进到终态
        return ActionResult.SUCCESS;
    }
    @Override
    public void tick(MyAction action, FakePlayerHandle handle) {
        // 仅在 RETRY 后每 tick 调用
    }
    @Override
    public void cancel(MyAction action, FakePlayerHandle handle) {
    }
}

// 3. 注册（ModInitializer 中）
CRPIFakePlayerMod.dispatcher().register(ActionType.USE_ITEM, new MyExecutor());
```

注意：`ActionType` 为固定枚举，自定义逻辑复用现有类型并注册覆盖执行器（会替换该类型的默认执行器），或用独立分支判断。**不推荐**通过 Mixin 修改枚举。

## 9. FakePlayerHandle 可用能力

```java
player()                 // ServerPlayerEntity（原生实体）
name()                   // 假人名
world()                  // ServerWorld
inventory()              // PlayerInventory
currentScreenHandler()   // 当前容器（可能为 PlayerScreenHandler）
gameMode()               // GameMode
x()/y()/z()              // 坐标
yaw()/pitch()            // 朝向
isOnline()               // 未移除
control()                // Control API（§6）
navigation()             // Navigation API（§7）
```

## 10. Carpet 规则联动

| 规则 | 影响 |
|---|---|
| `fakePlayerActions` | 总开关（各执行器入口检查对应分组规则） |
| `fakePlayerCombat` / `fakePlayerItemUse` / `fakePlayerInteraction` / `fakePlayerMining` / `fakePlayerContainer` | 对应动作组开关，关闭时返回 `NO_PERMISSION` |
| `maxContainerScanRadius` | 扫描半径钳制 |

规则运行时可改（`/crpi-fakeplayer <规则> <值>`），无需重启。

## 11. 线程与安全约束

- **所有 API 只能在服务器主线程调用**（命令、ServerTick 事件、其它服务端逻辑内）。不要在异步线程调用（会抛 `Attempted to run on a non-existent server thread` 或世界状态不一致）
- 动作校验内置：目标存活/同世界/距离/区块已加载/规则开关
- 扫描不会加载区块；动作执行不会主动加载区块

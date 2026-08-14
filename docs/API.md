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
    modImplementation files("libs/crpi-fakeplayer-0.1.0.jar")
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

## 6. 扩展自定义 Action

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

## 7. FakePlayerHandle 可用能力

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
```

## 8. Carpet 规则联动

| 规则 | 影响 |
|---|---|
| `fakePlayerActions` | 总开关（各执行器入口检查对应分组规则） |
| `fakePlayerCombat` / `fakePlayerItemUse` / `fakePlayerInteraction` / `fakePlayerMining` / `fakePlayerContainer` | 对应动作组开关，关闭时返回 `NO_PERMISSION` |
| `maxContainerScanRadius` | 扫描半径钳制 |

规则运行时可改（`/crpi-fakeplayer <规则> <值>`），无需重启。

## 9. 线程与安全约束

- **所有 API 只能在服务器主线程调用**（命令、ServerTick 事件、其它服务端逻辑内）。不要在异步线程调用（会抛 `Attempted to run on a non-existent server thread` 或世界状态不一致）
- 动作校验内置：目标存活/同世界/距离/区块已加载/规则开关
- 扫描不会加载区块；动作执行不会主动加载区块

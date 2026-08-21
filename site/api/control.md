# Control API

Control API 通过 `FakePlayerHandle` 的 `control()` 方法访问，提供移动、视角、状态、背包、骑乘、命令执行等控制操作。

```java
FakePlayerControl ctrl = bot.control();
```

## 移动与视角

### moveTo — 移动到位置

```java
ActionResult result = ctrl.moveTo(new BlockPos(100, 64, 200), 1.0);
```

- 使用物理原生移动（通过 Carpet Action Pack 的 `setForward`/`setStrafing`）
- 返回值：移动中 `PASS`，到达 `SUCCESS`，障碍/超时/卡住 `FAIL`
- 容差 0.5 格
- 每 tick 步进 + 碰撞检测 + 20 tick 卡住检测 + 600 tick 超时

### lookAt — 看向位置

```java
ctrl.lookAt(new BlockPos(100, 64, 200));  // 看向方块
ctrl.lookAt(targetEntity);                 // 看向实体
```

平滑 2-tick 转向。

### pathfindTo — 直线寻路

```java
ctrl.pathfindTo(new BlockPos(100, 64, 200));
```

直线版本的寻路（真正的 A* 寻路请使用 Navigation API）。

## 状态控制

### sneak — 潜行

```java
ctrl.sneak(true);   // 开始潜行
ctrl.sneak(false);  // 取消潜行
```

### sprint — 疾跑

```java
ctrl.sprint(true);   // 开始疾跑
ctrl.sprint(false);  // 取消疾跑
```

### jump — 跳跃

```java
ctrl.jump();
```

### teleportTo — 安全传送

```java
ctrl.teleportTo(new BlockPos(100, 64, 200));
```

安全验证：
- 下方必须是实心方块
- 传送点和上方必须是空气
- 自动下马
- 清除当前移动任务

## 背包操作

### getHeldItem — 获取手持物品

```java
ItemStackSnapshot mainHand = ctrl.getHeldItem(Hand.MAIN_HAND);
ItemStackSnapshot offHand = ctrl.getHeldItem(Hand.OFF_HAND);

String itemId = mainHand.itemId();  // 如 "minecraft:diamond_pickaxe"
int count = mainHand.count();
```

`ItemStackSnapshot` 是不可变快照。

### swapHands — 交换双手

```java
ctrl.swapHands();
```

### setHeldItem — 设置手持物品

```java
ctrl.setHeldItem(Hand.MAIN_HAND, itemStack);
```

### giveItem — 给予物品

```java
ctrl.giveItem(new ItemStack(Items.DIAMOND, 64));
```

使用原生物品栈叠加逻辑，不会覆盖已有物品。

## 骑乘

### mount — 骑乘

```java
ctrl.mount(entity);
```

### dismount — 下马

```java
ctrl.dismount();
```

### startRiding — 开始骑乘

```java
ctrl.startRiding(entity, force);
```

## 命令与表达

### executeCommand — 以假人身份执行命令

```java
ctrl.executeCommand("tp ~ ~10 ~");
ctrl.executeCommand("give @s diamond 64");
```

使用假人的位置、世界、权限执行。

### sendChatMessage — 发送聊天消息

```java
ctrl.sendChatMessage("Hello, world!");
```

以假人身份广播聊天消息。

### playSound — 播放音效

```java
ctrl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
```

## 环境查询

### getContainerInfo — 当前容器信息

```java
ContainerInfo info = ctrl.getContainerInfo();
if (info != null) {
    // 容器类型、位置等
}
```

### getNearbyContainers — 附近容器

```java
List<ContainerInfo> containers = ctrl.getNearbyContainers(16.0);
```

### interactBlock — 方块交互

```java
ctrl.interactBlock(new BlockPos(100, 64, 200), Direction.UP, Hand.MAIN_HAND);
```

全参数版本的方块交互。

## 属性设置

```java
ctrl.setGameMode(GameMode.SURVIVAL);
ctrl.setHealth(20.0);
ctrl.setFoodLevel(20);
ctrl.addExperience(100);
```

## 单任务模型

::: warning 重要
每个假人同一时间只能有一个活跃的移动/视角任务。新任务会自动取消旧任务。
:::

- 移动任务：每 tick 位置步进 + `blocksMovement()` 碰撞检测
- 卡住检测：20 tick 无显著位移 → 失败
- 超时：600 tick（30 秒）→ 失败

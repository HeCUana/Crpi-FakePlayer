# CRPI-FakePlayer 实现计划

> 状态：**V0.1~V0.4 全部完成并实测通过（2026-08-13）**。目标 MC 1.21.11 / Fabric Loader 0.19.x / Carpet 1.4.194，纯服务端（`environment: "server"`），客户端零安装。

## 1. 环境确认（复用 D:\AiPEX\CrpiCarpet 的已验证配置）

| 项 | 值 | 来源 |
|---|---|---|
| Minecraft | 1.21.11 | CrpiCarpet gradle.properties |
| Yarn mappings | 1.21.11+build.6 | 同上 |
| Fabric Loader | 0.19.3 | 同上 |
| Carpet | 1.4.194（Modrinth maven） | 同上 |
| Fabric Loom | 1.17.17 | CrpiCarpet build.gradle |
| Java | 21（JDK: C:\Program Files\Zulu\zulu-21） | AGENTS.md |
| Carpet 源码参考 | D:\AiPEX\CrpiCarpet\fabric-carpet-1.4.194 | 本地仓库 |

新项目只需改：mod id = `crpi-fakeplayer`，包名 `com.crpi.fakeplayer`，去掉 CrpiCarpet 的 mixin/recorder 代码。

## 2. 已核实的 1.21.11 API（从 yarn sources jar 逐条确认）

### 交互核心（ServerPlayerInteractionManager）
- `processBlockBreakingAction(BlockPos, PlayerActionC2SPacket.Action, Direction, int, int)` — DIG 起始/继续/停止的原生入口（Carpet 假人 attack 就是调它）
- `finishMining(BlockPos, int sequence, String reason)` — 挖掘完成
- `tryBreakBlock(BlockPos)` — 立即破坏（creative）
- `interactBlock(ServerPlayerEntity, World, ItemStack, Hand, BlockHitResult)` — USE 入口（放置/开箱/按钮/门全走这里）
- `interactItem(ServerPlayerEntity, World, ItemStack, Hand)` — USE_ITEM 入口（空手使用物品）

### 玩家行为（ServerPlayerEntity / PlayerEntity / LivingEntity）
- `ServerPlayerEntity.attack(Entity)` — ATTACK 原生入口（伤害/击退/附魔全包含）
- `ServerPlayerEntity.dropItem(ItemStack, boolean dropAtSelf, boolean retainOwnership)` — DROP_ITEM 原生入口
- `PlayerEntity.interact(Entity, Hand)` — INTERACT_ENTITY 原生入口（村民交易/骑乘/喂食全包含）
- `ServerPlayerEntity.closeHandledScreen()` — CLOSE_GUI（内部会走 ScreenHandler.onClosed 完整生命周期）
- `LivingEntity.setCurrentHand(Hand)` / `stopUsingItem()` / `isUsingItem()` — 长按物品的开始/结束
- **关键确认**：`LivingEntity.tick()` 内自动调用 `tickActiveItemStack()`（sources 行 2762），假人是真实 ServerPlayerEntity 会被正常 tick ⇒ **USE_ITEM 长按→自动推进→USE_RELEASE 全程无需 Mixin、无需自己写 tick 逻辑**

### 容器（V0.3）
- `ScreenHandler.onSlotClick(int slot, int button, SlotActionType, PlayerEntity)` — GUI_CLICK 原生入口
- `ScreenHandler.onClosed(PlayerEntity)` — 由 closeHandledScreen 自动触发
- `SlotActionType`：PICKUP / QUICK_MOVE / SWAP / CLONE / THROW / QUICK_CRAFT / PICKUP_ALL（标准枚举未变）

### Carpet 假人适配点
- 假人实现类：`carpet.patches.EntityPlayerMPFake`（extends ServerPlayer，注册在 PlayerManager）
- 获取：`server.getPlayerManager().getPlayer(name)` + `instanceof EntityPlayerMPFake` 校验（同 CrpiCarpet 的 CarpetIntegration 做法）
- 扩展注册：`CarpetServer.manageExtension(this)` + `ModInitializer`（CrpiCarpet 已验证模式）

## 3. Mixin 评估：零 Mixin

上述 9 种动作在 1.21.11 全部有公开 API 入口，**不需要任何 Mixin**。这也绕开了 CrpiCarpet 目前正在排查的"mixin 注入在外部服务器上不生效"问题（见其 AGENTS.md）。

## 4. 包结构（最终形态，按阶段递进实现）

```
com.crpi.fakeplayer/
├── CRPIFakePlayerMod          # ModInitializer + CarpetExtension 入口
├── action/
│   ├── Action                 # 抽象：id/type/player/tick/state/result
│   ├── ActionType             # 9 种枚举
│   ├── ActionResult           # SUCCESS/PASS/FAIL/RETRY/SKIP/ABORT/INVALID_TARGET/OUT_OF_RANGE/NO_PERMISSION/INVALID_STATE
│   ├── ActionState            # CREATED/QUEUED/STARTED/RUNNING/SUCCESS/FAILED/CANCELLED
│   ├── ActionExecutor<T>      # execute/tick/cancel 接口（瞬时 action 只需 execute）
│   └── ActionDispatcher       # type → executor 注册表
├── scheduler/
│   ├── ActionScheduler        # 每 server tick 由 Carpet onTick 驱动（无新线程）
│   ├── ActionQueue            # 每假人一队列：immediate/scheduled/sequential
│   └── ActionContext          # 执行上下文（bot/世界/权限快照/链深度）
├── fakeplayer/
│   ├── FakePlayerHandle       # 对外句柄（封装 ServerPlayerEntity + 状态检查）
│   ├── FakePlayerAdapter      # Carpet 适配层：name→EntityPlayerMPFake、在线/维度/库存/主手/ScreenHandler/位置
│   └── FakePlayerRegistry     # handle 缓存
├── action/impl/               # 每动作：Action 参数类 + Executor（DIG 走 mining 会话）
├── mining/
│   ├── MiningSession          # START→RUNNING→FINISH/CANCEL，进度驱动走原生 processBlockBreakingAction
│   └── MiningManager
├── container/
│   ├── ContainerContext       # syncId/handler/开箱 tick/slot 数
│   └── ContainerManager
├── command/FakePlayerCommand  # /fp 调试命令（仅薄壳，调 Action API）
├── config/CRPIFakePlayerSettings  # Carpet Rules
└── api/FakePlayerActions      # 流畅 API：FakePlayerActions.of(bot).dig(...).execute()
```

## 5. 分阶段计划

### V0.1 框架 + 4 个瞬时动作（先行验证零 Mixin 路线可行）
1. Mod 骨架（build.gradle/fabric.mod.json/gradle.properties）+ Carpet 集成
2. Action/ActionType/ActionState/ActionResult/Dispatcher/Scheduler/Queue + FakePlayerAdapter
3. ATTACK → `player.attack(entity)`（先查存活/同世界/距离 ≤4）
4. DROP_ITEM → `player.dropItem(stack, false, true)`（主手/副手/单/组）
5. CLOSE_GUI → `player.closeHandledScreen()`（仅当 currentScreenHandler 非 PlayerScreenHandler）
6. USE_ITEM → `interactionManager.interactItem(...)`
7. 命令：`/fp list|info|attack|drop|close|useitem`（op ≥2）
8. **验收**：Survival 测试服 RCON 逐条验证（假人攻击实体掉血、丢物品、开箱关箱、使用物品）

### V0.2 DIG / USE / INTERACT_ENTITY
- DIG：MiningSession（START → 每 tick `processBlockBreakingAction(pos, START_DESTROY_BLOCK, dir, maxY, seq)` 持续推进 → `getDestroyProgress` 或原生机制破坏；用 `finishMining` 收尾）。不自己算硬度/工具速度——**全部原生**
- USE：`interactBlock` + 构造 BlockHitResult（pos/dir/hitPos/hand 全参数）
- INTERACT_ENTITY：`player.interact(entity, hand)` + 距离/存活校验
- **验收**：dirt/stone/wood 挖掘（空手+工具）、开箱、按钮、拉杆、放置方块、与村民/动物交互

### V0.3 容器操作
- ContainerContext / ContainerManager：USE 开箱 → 记录 handler → GUI_CLICK → `handler.onSlotClick(...)` → CLOSE_GUI
- 校验：handler 存在、slot 合法、click 前后 `handler.canUse(player)`
- 命令：`/fp gui info|click|close|list`
- **验收**：Chest/Barrel/Furnace/Hopper/ShulkerBox 的 PICKUP/QUICK_MOVE/SWAP/THROW

### V0.4 Stateful Item Use
- `setCurrentHand` 开始 → 依赖原生 `tickActiveItemStack` 推进 → 到时长或指令 → `stopUsingItem()`
- USE_RELEASE 输入 duration；StatefulAction 接口（start/tick/isFinished/finish/cancel）
- **验收**：弓拉满射出、盾持盾、吃食物恢复

## 6. Carpet Rules（7 条，默认全开）

`fakePlayerActions` / `fakePlayerMining` / `fakePlayerInteraction` / `fakePlayerContainer` / `fakePlayerCombat` / `fakePlayerItemUse` / `fakePlayerDebug`（中文+英文 lang，沿用 CrpiCarpet 的语言文件模式）

## 7. 安全与限制

- 权限：命令 op≥2；容器/Debug 命令 op≥3
- 限额：maxActionsPerTick(64) / maxQueueLength(64) / maxConcurrentActions(16) / maxActionChainDepth(8)
- 执行前校验：目标 chunk loaded（不主动加载）、距离检查、假人在线检查
- 日志：debug 规则开启时输出 `[CRPI-FakePlayer] player=... action=... state=...`，默认静默

## 8. 测试方案

- 测试服务器：D:\AiPEX\Survival（MCDR + RCON 31001，CrpiCarpet 已验证的工作流）；每阶段 build → 部署 → RCON 实测 → 日志核对
- 测试矩阵按需求文档第 20 节逐项执行

## 9. 待确认问题

1. 命令前缀用 `/fp` 还是 `/crpifp`？（Carpt 附属已有 `/player`，`/fp` 简洁但与部分 mod 可能冲突）
2. 规则名是否按第 6 节命名？
3. 包名用 `com.crpi.fakeplayer`（需求文档里是 `com.creartpi.crpifakeplayer`，疑似笔误）
4. mod 版本号从 0.1.0 起

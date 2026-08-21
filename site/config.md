# 配置规则

CRPI-FakePlayer 通过 Carpet 规则系统进行配置。所有规则可在运行时修改。

## 修改规则

```
/crpi-fakeplayer <规则名> <值>
```

**示例：**
```
/crpi-fakeplayer fakePlayerActions true
/crpi-fakeplayer maxQueueLength 128
/crpi-fakeplayer fakePlayerDebug true
```

## 规则列表

| 规则 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `fakePlayerActions` | boolean | `true` | 主开关：禁用后所有命令和行为不可用 |
| `fakePlayerCombat` | boolean | `true` | ATTACK 行为开关 |
| `fakePlayerItemUse` | boolean | `true` | USE_ITEM / USE_RELEASE 行为开关 |
| `fakePlayerInteraction` | boolean | `true` | USE / INTERACT_ENTITY 行为开关 |
| `fakePlayerMining` | boolean | `true` | DIG 行为开关 |
| `fakePlayerContainer` | boolean | `true` | GUI_CLICK 和容器操作开关 |
| `fakePlayerDebug` | boolean | `false` | 详细行为日志（调试用） |
| `maxQueueLength` | int | `64` | 每假人行为队列最大长度 |
| `maxConcurrentActions` | int | `16` | 每假人并发有状态行为上限 |
| `maxContainerScanRadius` | int | `16` | 容器扫描最大半径 |

## 规则层级

```
fakePlayerActions (主开关)
├── fakePlayerCombat        → ATTACK
├── fakePlayerItemUse       → USE_ITEM, USE_RELEASE
├── fakePlayerInteraction   → USE, INTERACT_ENTITY
├── fakePlayerMining        → DIG
└── fakePlayerContainer     → GUI_CLICK, 容器操作
```

::: warning
将 `fakePlayerActions` 设为 `false` 会禁用所有行为，无论子规则如何设置。
:::

## 硬编码常量

以下常量在代码中硬编码，不可通过规则修改：

| 常量 | 值 | 说明 |
|---|---|---|
| `MAX_ACTION_CHAIN_DEPTH` | `8` | Action 链最大深度 |
| `MAX_PIPELINE_RETRIES` | `8` | Pipeline 最大重试次数 |
| `MAX_NODES` | `1000` | A* 寻路最大节点数 |
| `MAX_REPATHES` | `3` | 最大重新寻路次数 |
| `STUCK_TICKS` | `40` | 卡住检测 Tick 数 |
| `MOVE_TIMEOUT_TICKS` | `600` | 移动超时 Tick 数（30 秒） |

## 调试模式

开启 `fakePlayerDebug` 后，会在日志中输出：

- 行为队列变化
- 每个行为的执行细节
- 寻路计算过程
- 卡住检测和重新寻路事件
- 容器操作详情

```
/crpi-fakeplayer fakePlayerDebug true
```

日志输出到服务端的 `logs/latest.log`。

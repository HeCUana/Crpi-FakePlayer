# 命令参考

所有命令挂载在 `/crpi fp` 下，需要 OP 等级 2 权限。

## 行为命令

### 攻击

```
/crpi fp attack <player> <entity>
```

让假人攻击指定实体。使用原生伤害/击退/附魔/冷却逻辑。

**参数：**
- `player` — 假人名称
- `entity` — 目标实体选择器（如 `@e[type=zombie,limit=1,sort=nearest]`）

**示例：**
```
/crpi fp attack MyBot @e[type=zombie,limit=1,sort=nearest]
```

### 挖掘

```
/crpi fp dig <player> <pos> [face]
```

挖掘指定位置的方块。使用原生挖掘硬度/工具/附魔/水下惩罚公式。

**参数：**
- `player` — 假人名称
- `pos` — 方块坐标（`x y z`）
- `face` — 挖掘面（默认 `up`）：`up` / `down` / `north` / `south` / `east` / `west`

**示例：**
```
/crpi fp dig MyBot 100 64 200 up
```

### 使用物品

```
/crpi fp useitem <player> [off]
```

使用主手（或副手）持有的物品。

**参数：**
- `player` — 假人名称
- `off` — 可选，加 `off` 则使用副手

**示例：**
```
/crpi fp useitem MyBot
/crpi fp useitem MyBot off
```

### 右键交互

```
/crpi fp use <player> <pos> [face]
```

右键点击方块（放置、开门、打开工作台等）。

**参数：**
- `player` — 假人名称
- `pos` — 方块坐标
- `face` — 点击面（默认 `up`）

### 实体交互

```
/crpi fp interact <player> <entity> [off]
```

与实体交互（村民交易、骑乘、喂食等）。

### 长按释放

```
/crpi fp userelease <player> [off] [tick]
```

长按物品后释放（弓满弦/进食/盾牌格挡）。

**参数：**
- `player` — 假人名称
- `off` — 可选，使用副手
- `tick` — 可选，长按 tick 数（默认 20）

### 丢弃物品

```
/crpi fp drop <player> [off|all]
```

- 不加参数：丢弃主手物品
- `off`：丢弃副手物品
- `all`：丢弃所有物品

### 关闭 GUI

```
/crpi fp close <player>
```

关闭假人当前打开的容器/界面。

---

## 控制命令

### 移动

```
/crpi fp move <player> <pos> [speed]
```

移动到指定位置。使用物理原生移动（非传送）。

**参数：**
- `pos` — 目标坐标
- `speed` — 移动速度（默认 1.0）

### 视角

```
/crpi fp lookat <player> <pos>
```

让假人看向指定位置（平滑 2-tick 转向）。

### 跳跃

```
/crpi fp jump <player>
```

执行一次跳跃。

### 传送

```
/crpi fp teleport <player> <pos>
```

安全传送到指定位置（验证着陆点安全性：下方实心、上方空气）。

### 潜行

```
/crpi fp sneak <player> [off]
```

切换潜行状态。加 `off` 取消潜行。

### 疾跑

```
/crpi fp sprint <player> [off]
```

切换疾跑状态。加 `off` 取消疾跑。

### 交换双手

```
/crpi fp swap <player>
```

交换主手和副手物品。

### 执行命令

```
/crpi fp exec <player> <command>
```

以假人身份执行命令（使用假人的位置/世界/权限）。

**示例：**
```
/crpi fp exec MyBot tp ~ ~10 ~
/crpi fp exec MyBot give @s diamond 64
```

---

## 导航命令

### A* 寻路

```
/crpi fp goto <player> <pos> [near <radius>]
```

A* 寻路到目标位置。

**参数：**
- `pos` — 目标坐标
- `near <radius>` — 可选，到达半径内即算成功（默认精确到达）

**示例：**
```
/crpi fp goto MyBot 200 64 300
/crpi fp goto MyBot 200 64 300 near 5
```

### 多目标寻路

```
/crpi fp gotoany <player> <pos1> <pos2> ...
```

寻路到任意一个可达的目标位置。

### 跟随实体

```
/crpi fp follow <player> <entity>
```

持续跟随指定实体。目标移动时自动重新寻路，目标消失则停止。

### 路径点导航

```
/crpi fp followpath <player> <x,z> [<x,z> ...]
```

按路径点列表顺序移动（仅支持同高度点）。

**示例：**
```
/crpi fp followpath MyBot 100,200 150,200 150,250
```

### 停止导航

```
/crpi fp navstop <player>
```

停止当前导航。

### 导航状态

```
/crpi fp navstatus <player>
```

查看假人的导航状态：`IDLE` / `CALCULATING` / `RUNNING` / `SUCCESS` / `FAILED` / `STUCK` / `CANCELLED`。

---

## 容器命令

### 容器操作

```
/crpi fp gui info <player>       # 当前容器信息
/crpi fp gui list <player>       # 容器槽位列表
/crpi fp gui click <player> <slot> [button] [mode]  # 点击槽位
/crpi fp gui close <player>      # 关闭容器
```

::: tip
容器操作需要先通过 `/crpi fp use` 右键打开容器，然后再执行 GUI 命令。
:::

### 扫描容器

```
/crpi fp scancontainers <player> <radius>
```

扫描假人周围指定半径内的容器，返回容器类型和坐标。

---

## 其他命令

### 列出假人

```
/crpi fp list
```

列出所有在线的假人。

### 假人信息

```
/crpi fp info <player>
```

查看假人的详细状态：位置、维度、游戏模式、当前打开的界面等。

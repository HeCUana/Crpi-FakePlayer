# 快速开始

本指南将帮助你在 5 分钟内让假人动起来。

## 前置条件

| 依赖 | 版本要求 |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.19.3 |
| Carpet | 1.4.194 |
| Java | 21 |

## 第一步：安装

1. 下载最新版本的 `crpi-fakeplayer-x.x.x.jar`
2. 将 jar 文件放入服务端的 `mods/` 目录
3. 确保 `mods/` 中也有 Fabric API 和 Carpet 的 jar
4. 启动服务端

详见 [安装部署](./installation.md)。

## 第二步：生成假人

在服务端控制台或游戏中执行：

```
/player MyBot spawn
```

这会使用 Carpet 在当前位置生成一个名为 `MyBot` 的假人。

## 第三步：执行第一个行为

### 使用命令

```
/crpi fp dig MyBot 100 64 200 up
```

让假人挖掘坐标 `(100, 64, 200)` 处的方块。

### 使用 API（通过 MCDR 或其他服务端模组）

```java
// 获取假人句柄
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// 挖掘方块
FakePlayerActions.of(bot)
    .dig(new BlockPos(100, 64, 200), Direction.UP)
    .execute();
```

## 第四步：更多行为

```java
// 攻击实体
FakePlayerActions.of(bot).attack(targetEntity).execute();

// 丢弃物品
FakePlayerActions.of(bot).drop().execute();

// 移动到目标位置
bot.control().moveTo(new BlockPos(150, 64, 200), 1.0);

// A* 寻路
bot.navigation().gotoBlock(new BlockPos(200, 64, 300));

// 跟随实体
bot.navigation().follow(targetEntity, 3);
```

## 下一步

- [命令参考](./commands.md) — 了解所有 `/crpi fp` 命令
- [Action API](/api/actions.md) — 深入了解 9 种行为类型
- [Control API](/api/control.md) — 移动、视角、背包等控制操作
- [Navigation API](/api/navigation.md) — A* 寻路与物理原生移动
- [配置规则](/config.md) — 调整模组行为

---
layout: home

hero:
  name: CRPI-FakePlayer
  text: Fabric + Carpet 假人行为驱动模组
  tagline: 通过原生 Minecraft 逻辑驱动 Carpet 假人执行真实行为 — 挖掘、战斗、寻路、容器操作，零 Mixin、零新线程。
  actions:
    - theme: brand
      text: 快速开始
      link: /guide/getting-started
    - theme: alt
      text: API 参考
      link: /api/overview
    - theme: alt
      text: GitHub
      link: https://github.com/HeCUana/CRPI-FakePlayer

features:
  - icon: ⚡
    title: 行为系统 (Action API)
    details: 9 种原生行为类型 — 攻击、挖掘、使用物品、右键交互、容器操作等。所有伤害、附魔、冷却、挖掘公式均通过 Minecraft 原生 API 处理。
  - icon: 🎮
    title: 控制系统 (Control API)
    details: 移动、视角、潜行、跳跃、传送、背包操作、骑乘、命令执行。通过 Carpet Action Pack 实现物理原生移动，而非传送。
  - icon: 🗺️
    title: 寻路导航 (Navigation API)
    details: A* 寻路引擎 + 8 种物理原生移动类型。支持方块破坏/放置、跑酷跳跃、实体跟随。卡住自动重新寻路。
  - icon: 🔗
    title: ActionPipeline
    details: 行为编排组合器 — 前置条件检查、超时控制、失败重试、成功/失败回调。构建复杂的行为工作流。
  - icon: 🛡️
    title: 零 Mixin / 零新线程
    details: 所有行为通过原生 Minecraft 入口点执行。伤害、附魔、冷却、挖掘公式、物品逻辑全部保持原生。不使用 Mixin，不创建新线程。
  - icon: ⏱️
    title: Tick 驱动架构
    details: 所有持续性行为在服务端主线程上按 Tick 步进。通过 Carpet onTick 钩子驱动 ActionScheduler → ControlManager → NavigationRegistry 链路。
---

## 快速示例

```java
// 获取假人句柄
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// 挖掘方块
FakePlayerActions.of(bot)
    .dig(new BlockPos(100, 64, 200), Direction.UP)
    .execute();

// A* 寻路到目标位置
bot.navigation().gotoBlock(new BlockPos(200, 64, 300));

// 使用 ActionPipeline 编排复杂行为
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .timeout(100)
    .retry(3)
    .onSuccess(r -> bot.control().sendChatMessage("挖完了!"))
    .execute(bot);
```

## 技术栈

<div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px;">
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Minecraft 1.21.11</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Fabric Loader 0.19.3</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Carpet 1.4.194</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Java 21</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">MIT License</span>
</div>

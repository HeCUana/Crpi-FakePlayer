---
layout: home

hero:
  name: CRPI-FakePlayer
  text: Fake Player Behavior Driver Mod
  tagline: Drive Carpet fake players through realistic Minecraft behaviors — mining, combat, pathfinding, container operations. Zero Mixin, zero new threads.
  actions:
    - theme: brand
      text: Getting Started
      link: /en/guide/getting-started
    - theme: alt
      text: API Reference
      link: /en/api/overview
    - theme: alt
      text: GitHub
      link: https://github.com/HeCUana/CRPI-FakePlayer

features:
  - icon: ⚡
    title: Action System (Action API)
    details: 9 native behavior types — attack, dig, use item, right-click interact, container operations. All damage, enchantments, cooldowns, and mining formulas use native Minecraft APIs.
  - icon: 🎮
    title: Control System (Control API)
    details: Movement, look, sneak, jump, teleport, inventory, riding, command execution. Physics-native movement via Carpet Action Pack, not teleportation.
  - icon: 🗺️
    title: Pathfinding (Navigation API)
    details: A* pathfinding engine + 8 physics-native movement types. Supports block breaking/placing, parkour jumps, entity following. Auto-repath on stuck.
  - icon: 🔗
    title: ActionPipeline
    details: Behavior orchestration combinator — preconditions, timeout control, failure retry, success/failure callbacks. Build complex behavior workflows.
  - icon: 🛡️
    title: Zero Mixin / Zero Threads
    details: All behaviors execute through native Minecraft entry points. Damage, enchantments, cooldowns, mining formulas, item logic all stay native.
  - icon: ⏱️
    title: Tick-Driven Architecture
    details: All sustained behaviors step on the server main thread per tick. Driven by Carpet onTick hook through ActionScheduler → ControlManager → NavigationRegistry chain.
---

## Quick Example

```java
// Get fake player handle
FakePlayerHandle bot = FakePlayerAdapter.resolve(server, "MyBot");

// Dig a block
FakePlayerActions.of(bot)
    .dig(new BlockPos(100, 64, 200), Direction.UP)
    .execute();

// A* pathfind to target
bot.navigation().gotoBlock(new BlockPos(200, 64, 300));

// Orchestrate with ActionPipeline
new ActionPipeline<>(new DigAction(bot, tick, pos, face))
    .require(HasItem.of("minecraft:diamond_pickaxe"))
    .timeout(100)
    .retry(3)
    .onSuccess(r -> bot.control().sendChatMessage("Done!"))
    .execute(bot);
```

## Tech Stack

<div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px;">
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Minecraft 1.21.11</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Fabric Loader 0.19.3</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Carpet 1.4.194</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">Java 21</span>
  <span style="padding: 4px 12px; background: var(--vp-c-brand-soft); border-radius: 6px; font-size: 0.9em;">MIT License</span>
</div>

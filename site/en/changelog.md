# Changelog

## 0.4.0

**ActionPipeline Orchestration**

- Added `ActionPipeline` combinator: preconditions, timeout, retry, success/failure callbacks
- Added `HasItem` predicate: inventory item checking
- Added `ActionHooks`: behavior lifecycle hooks
- Added `PipelineRun`: Pipeline execution state tracking

## 0.3.0

**Navigation Pathfinding System**

- Added A* pathfinding engine (1000 node cap, 20ms calculation budget)
- Added 8 physics-native movement types: Traverse, Ascend, Descend, Diagonal, Fall, Parkour, Break, Place
- Added Goal system: GoalBlock, GoalNear, GoalComposite, GoalFollow
- Added `FakePlayerMovementController`: Carpet Action Pack bridge
- Added NavigationProfile: configurable pathfinding behavior
- Added CostModel + Favoring: danger costs + position penalties
- Added stuck detection + auto-repath (max 3 times)

## 0.2.0

**Control System**

- Added `FakePlayerControl`: 24+ control methods
- Added MoveTask / LookTask: continuous movement and look tasks
- Added physics-native movement (not teleportation)
- Added safe teleport validation
- Added inventory operations (getHeldItem, swapHands, setHeldItem, giveItem)
- Added riding operations (mount, dismount)
- Added command execution (executeCommand, sendChatMessage)
- Added environment queries (getContainerInfo, getNearbyContainers)

## V0.4

**USE_RELEASE Stateful Behavior**

- Added `UseReleaseAction`: hold item then release
- Added `ItemUseSession`: item use state machine

## V0.3

**Container Operations**

- Added `GuiClickAction`: container slot click
- Added `ContainerContext` / `ContainerManager`: container operation context
- Added `ContainerScanner`: container scanning

## V0.2

**DIG / USE / INTERACT_ENTITY**

- Added `DigAction`: block digging (native hardness/tool/enchantment formula)
- Added `UseAction`: right-click interaction
- Added `InteractEntityAction`: entity interaction
- Added `MiningSession`: mining state machine

## V0.1

**Framework + Basic Behaviors**

- Behavior framework: `ActionType`, `Action`, `ActionDispatcher`, `ActionExecutor`
- `ActionScheduler`: behavior scheduler (immediate/scheduled/sequential queue)
- `FakePlayerHandle` / `FakePlayerAdapter`: fake player handle adapter
- `FakePlayerActions`: Fluent facade
- Basic behaviors: ATTACK, DROP_ITEM, CLOSE_GUI, USE_ITEM

---

## Future Plans

- `ActionSequence`: chain behavior execution
- Swimming/water pathfinding
- Replay/TAS bridge
- AI behavior decisions
- Litematica bridge

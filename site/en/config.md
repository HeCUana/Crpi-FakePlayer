# Configuration

CRPI-FakePlayer is configured through the Carpet rule system. All rules can be modified at runtime.

## Modify Rules

```
/crpi-fakeplayer <rule_name> <value>
```

**Examples:**
```
/crpi-fakeplayer fakePlayerActions true
/crpi-fakeplayer maxQueueLength 128
/crpi-fakeplayer fakePlayerDebug true
```

## Rules

| Rule | Type | Default | Description |
|---|---|---|---|
| `fakePlayerActions` | boolean | `true` | Master switch: disables all commands and behaviors |
| `fakePlayerCombat` | boolean | `true` | ATTACK behavior switch |
| `fakePlayerItemUse` | boolean | `true` | USE_ITEM / USE_RELEASE behavior switch |
| `fakePlayerInteraction` | boolean | `true` | USE / INTERACT_ENTITY behavior switch |
| `fakePlayerMining` | boolean | `true` | DIG behavior switch |
| `fakePlayerContainer` | boolean | `true` | GUI_CLICK and container operations switch |
| `fakePlayerDebug` | boolean | `false` | Detailed behavior logging (for debugging) |
| `maxQueueLength` | int | `64` | Per-bot action queue max length |
| `maxConcurrentActions` | int | `16` | Per-bot concurrent stateful actions limit |
| `maxContainerScanRadius` | int | `16` | Container scan max radius |

## Rule Hierarchy

```
fakePlayerActions (master switch)
├── fakePlayerCombat        → ATTACK
├── fakePlayerItemUse       → USE_ITEM, USE_RELEASE
├── fakePlayerInteraction   → USE, INTERACT_ENTITY
├── fakePlayerMining        → DIG
└── fakePlayerContainer     → GUI_CLICK, container ops
```

::: warning
Setting `fakePlayerActions` to `false` disables all behaviors regardless of sub-rule settings.
:::

## Hardcoded Constants

These constants are hardcoded and cannot be modified via rules:

| Constant | Value | Description |
|---|---|---|
| `MAX_ACTION_CHAIN_DEPTH` | `8` | Action chain max depth |
| `MAX_PIPELINE_RETRIES` | `8` | Pipeline max retry count |
| `MAX_NODES` | `1000` | A* pathfinding max nodes |
| `MAX_REPATHES` | `3` | Max repath count |
| `STUCK_TICKS` | `40` | Stuck detection tick count |
| `MOVE_TIMEOUT_TICKS` | `600` | Move timeout tick count (30 seconds) |

## Debug Mode

Enabling `fakePlayerDebug` outputs detailed logs:

- Action queue changes
- Per-action execution details
- Pathfinding calculation process
- Stuck detection and repath events
- Container operation details

```
/crpi-fakeplayer fakePlayerDebug true
```

Logs are written to the server's `logs/latest.log`.

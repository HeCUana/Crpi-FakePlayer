package com.crpi.fakeplayer.command;

import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.AttackAction;
import com.crpi.fakeplayer.action.impl.CloseGuiAction;
import com.crpi.fakeplayer.action.impl.DigAction;
import com.crpi.fakeplayer.action.impl.DropItemAction;
import com.crpi.fakeplayer.action.impl.GuiClickAction;
import com.crpi.fakeplayer.action.impl.InteractEntityAction;
import com.crpi.fakeplayer.action.impl.UseAction;
import com.crpi.fakeplayer.action.impl.UseItemAction;
import com.crpi.fakeplayer.action.impl.UseReleaseAction;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.fakeplayer.FakePlayerAdapter;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.crpi.fakeplayer.container.ContainerContext;
import com.crpi.fakeplayer.container.ContainerManager;
import com.crpi.fakeplayer.container.ContainerScanResult;
import com.crpi.fakeplayer.container.ContainerScanner;
import com.crpi.fakeplayer.container.ItemStackInfo;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Debug/test entry point: {@code /crpi fp ...}.
 *
 * <p>The {@code fp} sub-command is attached to the existing {@code crpi}
 * root when CRPI Carpet is present, otherwise a standalone {@code crpi}
 * root is created so the command always works.
 */
public final class FakePlayerCommand {
    private static final Predicate<ServerCommandSource> OP =
        source -> CommandManager.ADMINS_CHECK.allows(source.getPermissions());

    private FakePlayerCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> existing = dispatcher.getRoot().getChild("crpi");
        if (existing == null) {
            dispatcher.register(CommandManager.literal("crpi").then(buildFp()));
        } else {
            ((LiteralCommandNode<ServerCommandSource>) existing).addChild(buildFp());
        }
    }

    private static LiteralCommandNode<ServerCommandSource> buildFp() {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> fp =
            CommandManager.literal("fp").requires(OP);

        fp.then(CommandManager.literal("list")
            .executes(FakePlayerCommand::list));

        fp.then(CommandManager.literal("info")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .executes(FakePlayerCommand::info)));

        fp.then(CommandManager.literal("attack")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("target", EntityArgumentType.entity())
                    .executes(FakePlayerCommand::attack))));

        fp.then(CommandManager.literal("drop")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.literal("off").executes(ctx -> drop(ctx, Hand.OFF_HAND, false)))
                .then(CommandManager.literal("all").executes(ctx -> drop(ctx, Hand.MAIN_HAND, true)))
                .executes(ctx -> drop(ctx, Hand.MAIN_HAND, false))));

        fp.then(CommandManager.literal("close")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .executes(FakePlayerCommand::closeGui)));

        fp.then(CommandManager.literal("useitem")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.literal("off").executes(ctx -> useItem(ctx, Hand.OFF_HAND)))
                .executes(ctx -> useItem(ctx, Hand.MAIN_HAND))));

        fp.then(CommandManager.literal("dig")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("face", StringArgumentType.word())
                        .suggests((c, b) -> {
                            for (String f : new String[]{"up", "down", "north", "south", "east", "west"}) {
                                b.suggest(f);
                            }
                            return b.buildFuture();
                        })
                        .executes(ctx -> dig(ctx, face(ctx))))
                    .executes(ctx -> dig(ctx, Direction.UP)))));

        fp.then(CommandManager.literal("use")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("face", StringArgumentType.word())
                        .suggests((c, b) -> {
                            for (String f : new String[]{"up", "down", "north", "south", "east", "west"}) {
                                b.suggest(f);
                            }
                            return b.buildFuture();
                        })
                        .executes(ctx -> useBlock(ctx, face(ctx))))
                    .executes(ctx -> useBlock(ctx, Direction.UP)))));

        fp.then(CommandManager.literal("interact")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("target", EntityArgumentType.entity())
                    .then(CommandManager.literal("off").executes(ctx -> interact(ctx, Hand.OFF_HAND)))
                    .executes(ctx -> interact(ctx, Hand.MAIN_HAND)))));

        fp.then(CommandManager.literal("userelease")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.literal("off")
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1))
                        .executes(ctx -> useRelease(ctx, Hand.OFF_HAND)))
                    .executes(ctx -> useRelease(ctx, Hand.OFF_HAND, 20)))
                .then(CommandManager.argument("duration", IntegerArgumentType.integer(1))
                    .executes(ctx -> useRelease(ctx, Hand.MAIN_HAND)))
                .executes(ctx -> useRelease(ctx, Hand.MAIN_HAND, 20))));

        fp.then(CommandManager.literal("scancontainers")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1))
                    .executes(FakePlayerCommand::scanContainers))));

        fp.then(CommandManager.literal("move")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("speed", DoubleArgumentType.doubleArg(0.1, 20.0))
                        .executes(FakePlayerCommand::moveTo))
                    .executes(ctx -> moveTo(ctx, 4.3)))));

        fp.then(CommandManager.literal("lookat")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(FakePlayerCommand::lookAt))));

        fp.then(CommandManager.literal("jump")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .executes(FakePlayerCommand::jump)));

        fp.then(CommandManager.literal("teleport")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(FakePlayerCommand::teleportTo))));

        fp.then(CommandManager.literal("sneak")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.literal("off").executes(ctx -> sneak(ctx, false)))
                .executes(ctx -> sneak(ctx, true))));

        fp.then(CommandManager.literal("sprint")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.literal("off").executes(ctx -> sprint(ctx, false)))
                .executes(ctx -> sprint(ctx, true))));

        fp.then(CommandManager.literal("swap")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .executes(FakePlayerCommand::swap)));

        fp.then(CommandManager.literal("exec")
            .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                    .executes(FakePlayerCommand::exec))));

        fp.then(CommandManager.literal("gui")
            .then(CommandManager.literal("info")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .executes(FakePlayerCommand::guiInfo)))
            .then(CommandManager.literal("list")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .executes(FakePlayerCommand::guiList)))
            .then(CommandManager.literal("close")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .executes(FakePlayerCommand::closeGui)))
            .then(CommandManager.literal("click")
                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                    .then(CommandManager.argument("slot", IntegerArgumentType.integer(0))
                        .then(CommandManager.argument("button", IntegerArgumentType.integer(0))
                            .then(CommandManager.argument("action", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    for (String a : new String[]{"pickup", "quick_move", "swap", "clone", "throw", "pickup_all"}) {
                                        b.suggest(a);
                                    }
                                    return b.buildFuture();
                                })
                                .executes(FakePlayerCommand::guiClick)))))));

        return fp.build();
    }

    private static FakePlayerHandle handle(CommandContext<ServerCommandSource> ctx, String playerArg) {
        String name;
        try {
            name = GameProfileArgumentType.getProfileArgument(ctx, playerArg).iterator().next().name();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return null;
        }
        FakePlayerHandle handle = FakePlayerAdapter.resolve(ctx.getSource().getServer(), name);
        if (handle == null) {
            ctx.getSource().sendError(Text.literal("Fake player not online: " + name));
        }
        return handle;
    }

    private static void send(CommandContext<ServerCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(() -> Text.literal(message), false);
    }

    private static int list(CommandContext<ServerCommandSource> ctx) {
        var players = ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
            .filter(FakePlayerAdapter::isFakePlayer)
            .map(p -> p.getGameProfile().name())
            .toList();
        send(ctx, players.isEmpty() ? "[CRPI-FakePlayer] No fake players online" : "[CRPI-FakePlayer] Fake players: " + String.join(", ", players));
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        send(ctx, "[CRPI-FakePlayer] " + h.name()
            + " pos=" + (int) h.x() + "," + (int) h.y() + "," + (int) h.z()
            + " dim=" + h.world().getRegistryKey().getValue()
            + " gamemode=" + h.gameMode().getId()
            + " screen=" + (h.currentScreenHandler() == null ? "none" : h.currentScreenHandler().getClass().getSimpleName()));
        return 1;
    }

    private static int attack(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        Entity target;
        try {
            target = EntityArgumentType.getEntity(ctx, "target");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return 0;
        }
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler()
            .runNow(new AttackAction(h, ctx.getSource().getServer().getTicks(), target));
        send(ctx, "[CRPI-FakePlayer] attack " + h.name() + " -> " + target.getName().getString() + " : " + result);
        return 1;
    }

    private static int drop(CommandContext<ServerCommandSource> ctx, Hand hand, boolean entireStack) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler()
            .runNow(new DropItemAction(h, ctx.getSource().getServer().getTicks(), hand, entireStack));
        send(ctx, "[CRPI-FakePlayer] drop " + h.name() + " hand=" + hand + " all=" + entireStack + " : " + result);
        return 1;
    }

    private static int closeGui(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler()
            .runNow(new CloseGuiAction(h, ctx.getSource().getServer().getTicks()));
        send(ctx, "[CRPI-FakePlayer] close " + h.name() + " : " + result);
        return 1;
    }

    private static int useItem(CommandContext<ServerCommandSource> ctx, Hand hand) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler()
            .runNow(new UseItemAction(h, ctx.getSource().getServer().getTicks(), hand));
        send(ctx, "[CRPI-FakePlayer] useitem " + h.name() + " hand=" + hand + " : " + result);
        return 1;
    }

    private static int dig(CommandContext<ServerCommandSource> ctx, Direction face) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
        DigAction action = new DigAction(h, ctx.getSource().getServer().getTicks(), pos, face);
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler().runNow(action);
        send(ctx, "[CRPI-FakePlayer] dig " + h.name() + " " + pos + " face=" + face + " : "
            + (result == ActionResult.PASS ? action.state().name() : result.name()));
        return 1;
    }

    private static int useBlock(CommandContext<ServerCommandSource> ctx, Direction face) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler()
            .runNow(new UseAction(h, ctx.getSource().getServer().getTicks(), pos, face));
        send(ctx, "[CRPI-FakePlayer] use " + h.name() + " " + pos + " face=" + face + " : " + result);
        return 1;
    }

    private static int useRelease(CommandContext<ServerCommandSource> ctx, Hand hand) {
        return useRelease(ctx, hand, IntegerArgumentType.getInteger(ctx, "duration"));
    }

    private static int useRelease(CommandContext<ServerCommandSource> ctx, Hand hand, int duration) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        UseReleaseAction action = new UseReleaseAction(h, ctx.getSource().getServer().getTicks(), hand, duration);
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler().runNow(action);
        send(ctx, "[CRPI-FakePlayer] userelease " + h.name() + " hand=" + hand + " duration=" + duration + " : "
            + (result == ActionResult.PASS ? action.state().name() : result.name()));
        return 1;
    }

    private static int scanContainers(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        if (radius > CRPIFakePlayerSettings.maxContainerScanRadius) {
            send(ctx, "[CRPI-FakePlayer] Invalid radius (max " + CRPIFakePlayerSettings.maxContainerScanRadius + ")");
            return 0;
        }
        List<ContainerScanResult> results = ContainerScanner.scan(h, radius);
        if (results.isEmpty()) {
            send(ctx, "[CRPI-FakePlayer] No containers found.");
            return 1;
        }
        StringBuilder sb = new StringBuilder("[CRPI-FakePlayer] Found ").append(results.size()).append(" containers.");
        for (int i = 0; i < results.size(); i++) {
            ContainerScanResult r = results.get(i);
            sb.append("\n[").append(i + 1).append("] ")
                .append(r.pos().getX()).append(" ").append(r.pos().getY()).append(" ").append(r.pos().getZ())
                .append(" type=").append(r.blockId())
                .append(" canOpen=").append(r.canOpen());
            if (!r.items().isEmpty()) {
                sb.append(" items:");
                for (ItemStackInfo item : r.items()) {
                    sb.append("\n  ").append(item.itemId()).append(" x").append(item.count());
                }
            }
        }
        send(ctx, sb.toString());
        return 1;
    }

    private static int moveTo(CommandContext<ServerCommandSource> ctx) {
        return moveTo(ctx, DoubleArgumentType.getDouble(ctx, "speed"));
    }

    private static int moveTo(CommandContext<ServerCommandSource> ctx, double speed) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ActionResult result = h.control().moveTo(BlockPosArgumentType.getBlockPos(ctx, "pos"), speed);
        send(ctx, "[CRPI-FakePlayer] move " + h.name() + " : " + result);
        return 1;
    }

    private static int lookAt(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ActionResult result = h.control().lookAt(BlockPosArgumentType.getBlockPos(ctx, "pos"));
        send(ctx, "[CRPI-FakePlayer] lookat " + h.name() + " : " + result);
        return 1;
    }

    private static int jump(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        send(ctx, "[CRPI-FakePlayer] jump " + h.name() + " : " + h.control().jump());
        return 1;
    }

    private static int teleportTo(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ActionResult result = h.control().teleportTo(BlockPosArgumentType.getBlockPos(ctx, "pos"));
        send(ctx, "[CRPI-FakePlayer] teleport " + h.name() + " : " + result);
        return 1;
    }

    private static int sneak(CommandContext<ServerCommandSource> ctx, boolean on) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        send(ctx, "[CRPI-FakePlayer] sneak " + h.name() + "=" + on + " : " + h.control().sneak(on));
        return 1;
    }

    private static int sprint(CommandContext<ServerCommandSource> ctx, boolean on) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        send(ctx, "[CRPI-FakePlayer] sprint " + h.name() + "=" + on + " : " + h.control().sprint(on));
        return 1;
    }

    private static int swap(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        send(ctx, "[CRPI-FakePlayer] swap " + h.name() + " : " + h.control().swapHands());
        return 1;
    }

    private static int exec(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        String command = StringArgumentType.getString(ctx, "command");
        ActionResult result = h.control().executeCommand(command);
        send(ctx, "[CRPI-FakePlayer] exec " + h.name() + " : " + result);
        return 1;
    }

    private static int guiInfo(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        ContainerContext context = ContainerContext.of(h);
        if (context == null) {
            send(ctx, "[CRPI-FakePlayer] " + h.name() + " has no container open");
            return 1;
        }
        send(ctx, "[CRPI-FakePlayer] " + h.name()
            + " container=" + context.handler().getClass().getSimpleName()
            + " syncId=" + context.syncId()
            + " slots=" + context.slotCount());
        return 1;
    }

    private static int guiList(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        List<String> lines = ContainerManager.describeContents(h);
        if (lines.isEmpty()) {
            send(ctx, "[CRPI-FakePlayer] " + h.name() + " no container open or container is empty");
            return 1;
        }
        send(ctx, "[CRPI-FakePlayer] " + h.name() + " contents: " + String.join(", ", lines));
        return 1;
    }

    private static int guiClick(CommandContext<ServerCommandSource> ctx) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        int button = IntegerArgumentType.getInteger(ctx, "button");
        SlotActionType type = switch (StringArgumentType.getString(ctx, "action")) {
            case "quick_move" -> SlotActionType.QUICK_MOVE;
            case "swap" -> SlotActionType.SWAP;
            case "clone" -> SlotActionType.CLONE;
            case "throw" -> SlotActionType.THROW;
            case "pickup_all" -> SlotActionType.PICKUP_ALL;
            default -> SlotActionType.PICKUP;
        };
        GuiClickAction action = new GuiClickAction(h, ctx.getSource().getServer().getTicks(), slot, button, type);
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler().runNow(action);
        send(ctx, "[CRPI-FakePlayer] gui click " + h.name() + " slot=" + slot + " button=" + button + " action=" + type + " : " + result);
        return 1;
    }

    private static Direction face(CommandContext<ServerCommandSource> ctx) {
        try {
            return direction(ctx, "face");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return Direction.UP;
        }
    }

    private static int interact(CommandContext<ServerCommandSource> ctx, Hand hand) {
        FakePlayerHandle h = handle(ctx, "player");
        if (h == null) {
            return 0;
        }
        Entity target;
        try {
            target = EntityArgumentType.getEntity(ctx, "target");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return 0;
        }
        ActionResult result = com.crpi.fakeplayer.CRPIFakePlayerMod.scheduler()
            .runNow(new InteractEntityAction(h, ctx.getSource().getServer().getTicks(), target, hand));
        send(ctx, "[CRPI-FakePlayer] interact " + h.name() + " -> " + target.getName().getString() + " : " + result);
        return 1;
    }

    private static Direction direction(CommandContext<ServerCommandSource> ctx, String arg) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return switch (StringArgumentType.getString(ctx, arg)) {
            case "down" -> Direction.DOWN;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> Direction.UP;
        };
    }
}

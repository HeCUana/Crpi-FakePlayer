package com.crpi.fakeplayer.control;

import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.container.ContainerContext;
import com.crpi.fakeplayer.container.ContainerScanResult;
import com.crpi.fakeplayer.container.ContainerScanner;
import com.crpi.fakeplayer.container.ItemStackInfo;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

/**
 * Per-fake-player control surface: movement, looking, state, inventory,
 * riding, commands and environment queries. Continuous operations (move /
 * look / path) run as {@link ControlTask}s driven by {@link ControlManager}
 * on the server tick — nothing blocks the server thread.
 */
public final class FakePlayerControl {
    private final FakePlayerHandle handle;
    private ControlTask task;

    public FakePlayerControl(FakePlayerHandle handle) {
        this.handle = handle;
    }

    public FakePlayerHandle handle() {
        return this.handle;
    }

    // ---- task plumbing (used by ControlManager) ----

    void tickTask(long currentTick) {
        if (this.task != null && !this.task.isFinished()) {
            this.task.tick(currentTick);
        }
    }

    private void startTask(ControlTask newTask) {
        if (this.task != null && !this.task.isFinished()) {
            this.task.cancel();
        }
        this.task = newTask;
    }

    public boolean hasActiveTask() {
        return this.task != null && !this.task.isFinished();
    }

    /** Cancels any running movement/look task (teleport does this). */
    public void clearTask() {
        if (this.task != null && !this.task.isFinished()) {
            this.task.cancel();
        }
        this.task = null;
    }

    private ActionResult taskResultOrPass() {
        if (this.task != null && this.task.isFinished()) {
            return this.task.result();
        }
        return ActionResult.PASS;
    }

    // ---- P0 ----

    /** Moves to a block. Returns PASS while moving, SUCCESS on arrival. */
    public ActionResult moveTo(BlockPos target, double speed) {
        if (target == null || speed <= 0) {
            return ActionResult.FAIL;
        }
        if (!handle().world().isChunkLoaded(target)) {
            return ActionResult.INVALID_TARGET;
        }
        startTask(new MoveTask(handle, List.of(target.toImmutable()), speed));
        return ActionResult.PASS;
    }

    /** Turns toward a block centre. Returns PASS while turning, then SUCCESS. */
    public ActionResult lookAt(BlockPos target) {
        if (target == null || !handle().world().isChunkLoaded(target)) {
            return ActionResult.FAIL;
        }
        return lookAt(new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5));
    }

    /** Turns toward an entity's bounding-box centre. */
    public ActionResult lookAt(Entity target) {
        if (target == null || !target.isAlive() || target.getEntityWorld() != handle().world()) {
            return ActionResult.FAIL;
        }
        return lookAt(target.getBoundingBox().getCenter());
    }

    private ActionResult lookAt(Vec3d point) {
        double[] facing = LookTask.facingToward(handle, point);
        startTask(new LookTask(handle, facing[0], facing[1]));
        return ActionResult.PASS;
    }

    public ActionResult sneak(boolean on) {
        handle().player().setSneaking(on);
        return ActionResult.SUCCESS;
    }

    public ActionResult sprint(boolean on) {
        handle().player().setSprinting(on);
        return ActionResult.SUCCESS;
    }

    public ActionResult jump() {
        handle().player().jump();
        return ActionResult.SUCCESS;
    }

    public ItemStackSnapshot getHeldItem(Hand hand) {
        ItemStack stack = handle().player().getStackInHand(hand);
        if (stack.isEmpty()) {
            return new ItemStackSnapshot(Identifier.of("minecraft", "air"), 0);
        }
        return new ItemStackSnapshot(Registries.ITEM.getId(stack.getItem()), stack.getCount());
    }

    public ActionResult swapHands() {
        PlayerInventory inventory = handle().inventory();
        int main = inventory.getSelectedSlot();
        int off = PlayerInventory.OFF_HAND_SLOT;
        ItemStack mainStack = inventory.getStack(main);
        ItemStack offStack = inventory.getStack(off);
        inventory.setStack(off, mainStack);
        inventory.setStack(main, offStack);
        inventory.markDirty();
        return ActionResult.SUCCESS;
    }

    public ActionResult teleportTo(BlockPos target) {
        if (target == null) {
            return ActionResult.FAIL;
        }
        ServerWorld world = handle().world();
        if (!world.isChunkLoaded(target)) {
            return ActionResult.INVALID_TARGET;
        }
        if (!isSafeStanding(world, target)) {
            return ActionResult.INVALID_TARGET;
        }
        clearTask();
        if (handle().player().hasVehicle()) {
            handle().player().stopRiding();
        }
        handle().player().teleportTo(new TeleportTarget(
            world,
            new Vec3d(target.getX() + 0.5, target.getY(), target.getZ() + 0.5),
            Vec3d.ZERO,
            handle().yaw(),
            handle().pitch(),
            TeleportTarget.NO_OP
        ));
        return ActionResult.SUCCESS;
    }

    private static boolean isSafeStanding(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isAir()
            && world.getBlockState(pos.up()).isAir()
            && !world.getBlockState(pos.down()).isAir();
    }

    // ---- P1 ----

    /** Moves through waypoints sequentially. */
    public ActionResult moveToPath(List<BlockPos> waypoints, double speed) {
        if (waypoints == null || waypoints.isEmpty() || speed <= 0) {
            return ActionResult.FAIL;
        }
        List<BlockPos> copy = new ArrayList<>(waypoints);
        startTask(new MoveTask(handle, copy, speed));
        return ActionResult.PASS;
    }

    public ActionResult setHeldItem(Hand hand, ItemStack stack) {
        if (stack == null) {
            return ActionResult.FAIL;
        }
        PlayerInventory inventory = handle().inventory();
        int slot = hand == Hand.OFF_HAND ? PlayerInventory.OFF_HAND_SLOT : inventory.getSelectedSlot();
        inventory.setStack(slot, stack.copy());
        inventory.markDirty();
        return ActionResult.SUCCESS;
    }

    /** Enhanced USE: fully parameterised block interaction with a hand. */
    public ActionResult interactBlock(BlockPos pos, Direction side, Hand hand) {
        if (pos == null || side == null || hand == null || !handle().world().isChunkLoaded(pos)) {
            return ActionResult.FAIL;
        }
        if (!handle().player().canInteractWithBlockAt(pos, 1.0)) {
            return ActionResult.OUT_OF_RANGE;
        }
        BlockHitResult hit = new BlockHitResult(pos.toCenterPos(), side, pos, false);
        net.minecraft.util.ActionResult vanilla = handle().player().interactionManager.interactBlock(
            handle().player(),
            handle().world(),
            handle().player().getStackInHand(hand),
            hand,
            hit
        );
        return vanilla == net.minecraft.util.ActionResult.FAIL ? ActionResult.FAIL : ActionResult.SUCCESS;
    }

    public ActionResult mount(Entity vehicle) {
        if (vehicle == null || !vehicle.isAlive() || vehicle.getEntityWorld() != handle().world()) {
            return ActionResult.INVALID_TARGET;
        }
        if (handle().player().hasVehicle()) {
            return ActionResult.INVALID_STATE;
        }
        return handle().player().startRiding(vehicle, true, true) ? ActionResult.SUCCESS : ActionResult.FAIL;
    }

    public ActionResult dismount() {
        if (!handle().player().hasVehicle()) {
            return ActionResult.INVALID_STATE;
        }
        handle().player().stopRiding();
        return ActionResult.SUCCESS;
    }

    /** Inserts an item following vanilla stacking rules (no overwriting). */
    public ActionResult giveItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ActionResult.FAIL;
        }
        PlayerInventory inventory = handle().inventory();
        ItemStack rest = stack.copy();
        // try merging into existing stacks first (main inventory = slots 0-35), then empty slots
        for (int i = 0; i < PlayerInventory.MAIN_SIZE && !rest.isEmpty(); i++) {
            ItemStack existing = inventory.getStack(i);
            if (ItemStack.areItemsAndComponentsEqual(existing, rest)) {
                int space = existing.getMaxCount() - existing.getCount();
                int moved = Math.min(space, rest.getCount());
                if (moved > 0) {
                    existing.increment(moved);
                    rest.decrement(moved);
                }
            }
        }
        if (rest.isEmpty()) {
            inventory.markDirty();
            return ActionResult.SUCCESS;
        }
        if (inventory.insertStack(rest)) {
            inventory.markDirty();
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    public ActionResult setHealth(double health) {
        double max = handle().player().getMaxHealth();
        if (health < 0 || health > max) {
            return ActionResult.INVALID_TARGET;
        }
        handle().player().setHealth((float) health);
        return ActionResult.SUCCESS;
    }

    public ActionResult setFoodLevel(int level) {
        if (level < 0 || level > 20) {
            return ActionResult.INVALID_TARGET;
        }
        HungerManager hunger = handle().player().getHungerManager();
        hunger.setFoodLevel(level);
        if (level == 20) {
            hunger.setSaturationLevel(5.0F);
        }
        return ActionResult.SUCCESS;
    }

    public ActionResult addExperience(int amount) {
        if (amount <= 0) {
            return ActionResult.INVALID_TARGET;
        }
        handle().player().addExperience(amount);
        return ActionResult.SUCCESS;
    }

    /** Runs a command AS the fake player (position/world/permission of the bot). */
    public ActionResult executeCommand(String command) {
        if (command == null || command.isBlank()) {
            return ActionResult.FAIL;
        }
        ServerCommandSource source = handle().player().getCommandSource();
        handle().world().getServer().getCommandManager().parseAndExecute(source, command);
        return ActionResult.SUCCESS;
    }

    // ---- P2 ----

    public ActionResult playSound(SoundEvent event) {
        if (event == null) {
            return ActionResult.FAIL;
        }
        handle().player().playSound(event, 1.0F, 1.0F);
        return ActionResult.SUCCESS;
    }

    public ActionResult setGameMode(GameMode mode) {
        if (mode == null) {
            return ActionResult.FAIL;
        }
        handle().player().changeGameMode(mode);
        return ActionResult.SUCCESS;
    }

    /** Broadcasts a chat line from the fake player. */
    public ActionResult sendChatMessage(String message) {
        if (message == null || message.isBlank()) {
            return ActionResult.FAIL;
        }
        Text text = Text.literal("<" + handle().name() + "> " + message);
        handle().world().getServer().getPlayerManager().broadcast(text, false);
        return ActionResult.SUCCESS;
    }

    /** Alias of {@link #mount} keeping the required method name. */
    public ActionResult startRiding(Entity vehicle, boolean forceControl) {
        return mount(vehicle);
    }

    /** Snapshot of the player's currently open container, or null. */
    public ContainerInfo getContainerInfo() {
        ContainerContext context = ContainerContext.of(handle);
        if (context == null) {
            return null;
        }
        ScreenHandler handler = context.handler();
        List<ItemStackSnapshot> items = new ArrayList<>();
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                items.add(new ItemStackSnapshot(Registries.ITEM.getId(stack.getItem()), stack.getCount()));
            }
        }
        return new ContainerInfo(handle().player().getBlockPos(), "screen", true, items);
    }

    /**
     * Straight-line movement toward a target. No obstacle avoidance yet;
     * a real pathfinder is planned.
     */
    public ActionResult pathfindTo(BlockPos target, double speed) {
        return moveTo(target, speed);
    }

    /** Environment: read-only container scan around the fake player. */
    public List<ContainerInfo> getNearbyContainers(double radius) {
        int clamped = (int) Math.min(radius, com.crpi.fakeplayer.config.CRPIFakePlayerSettings.maxContainerScanRadius);
        List<ContainerScanResult> results = ContainerScanner.scan(handle, clamped);
        List<ContainerInfo> info = new ArrayList<>(results.size());
        for (ContainerScanResult r : results) {
            List<ItemStackSnapshot> items = new ArrayList<>(r.items().size());
            for (ItemStackInfo item : r.items()) {
                items.add(new ItemStackSnapshot(item.itemId(), item.count()));
            }
            info.add(new ContainerInfo(r.pos(), r.blockId().toString(), r.canOpen(), items));
        }
        return info;
    }
}

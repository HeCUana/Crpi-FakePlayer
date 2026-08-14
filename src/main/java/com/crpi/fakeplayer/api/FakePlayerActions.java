package com.crpi.fakeplayer.api;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.action.impl.AttackAction;
import com.crpi.fakeplayer.action.impl.CloseGuiAction;
import com.crpi.fakeplayer.action.impl.DigAction;
import com.crpi.fakeplayer.action.impl.DropItemAction;
import com.crpi.fakeplayer.action.impl.InteractEntityAction;
import com.crpi.fakeplayer.action.impl.UseAction;
import com.crpi.fakeplayer.action.impl.UseItemAction;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.CRPIFakePlayerMod;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fluent entry point for driving a fake player:
 *
 * <pre>
 * FakePlayerActions.of(bot)
 *     .dig(pos, Direction.UP)
 *     .execute();
 * </pre>
 */
public final class FakePlayerActions {
    private final FakePlayerHandle handle;

    private FakePlayerActions(FakePlayerHandle handle) {
        this.handle = handle;
    }

    public static FakePlayerActions of(FakePlayerHandle handle) {
        return new FakePlayerActions(handle);
    }

    private long currentTick() {
        return this.handle.world().getServer().getTicks();
    }

    public AttackAction attack(Entity target) {
        return new AttackAction(this.handle, currentTick(), target);
    }

    public DropItemAction drop() {
        return new DropItemAction(this.handle, currentTick(), Hand.MAIN_HAND, false);
    }

    public DropItemAction drop(Hand hand, boolean entireStack) {
        return new DropItemAction(this.handle, currentTick(), hand, entireStack);
    }

    public CloseGuiAction closeGui() {
        return new CloseGuiAction(this.handle, currentTick());
    }

    public UseItemAction useItem(Hand hand) {
        return new UseItemAction(this.handle, currentTick(), hand);
    }

    public DigAction dig(BlockPos pos, Direction direction) {
        return new DigAction(this.handle, currentTick(), pos, direction);
    }

    public UseAction use(BlockPos pos, Direction direction) {
        return new UseAction(this.handle, currentTick(), pos, direction);
    }

    public InteractEntityAction interact(Entity target, Hand hand) {
        return new InteractEntityAction(this.handle, currentTick(), target, hand);
    }

    /**
     * Scans the loaded chunks around the fake player for containers.
     * Synchronous and read-only: no GUI is opened, no chunk is loaded.
     */
    public java.util.List<com.crpi.fakeplayer.container.ContainerScanResult> scanContainers(int radius) {
        int clamped = Math.min(radius, com.crpi.fakeplayer.config.CRPIFakePlayerSettings.maxContainerScanRadius);
        return com.crpi.fakeplayer.container.ContainerScanner.scan(this.handle, clamped);
    }

    // ---- Control API shortcuts (delegate to FakePlayerControl) ----

    public ActionResult moveTo(net.minecraft.util.math.BlockPos target, double speed) {
        return this.handle.control().moveTo(target, speed);
    }

    public ActionResult lookAt(net.minecraft.util.math.BlockPos target) {
        return this.handle.control().lookAt(target);
    }

    public ActionResult lookAt(Entity target) {
        return this.handle.control().lookAt(target);
    }

    public ActionResult sneak(boolean on) {
        return this.handle.control().sneak(on);
    }

    public ActionResult sprint(boolean on) {
        return this.handle.control().sprint(on);
    }

    public ActionResult jump() {
        return this.handle.control().jump();
    }

    public com.crpi.fakeplayer.control.ItemStackSnapshot getHeldItem(Hand hand) {
        return this.handle.control().getHeldItem(hand);
    }

    public ActionResult swapHands() {
        return this.handle.control().swapHands();
    }

    public ActionResult teleportTo(net.minecraft.util.math.BlockPos target) {
        return this.handle.control().teleportTo(target);
    }

    public ActionResult moveToPath(java.util.List<net.minecraft.util.math.BlockPos> waypoints, double speed) {
        return this.handle.control().moveToPath(waypoints, speed);
    }

    public ActionResult setHeldItem(Hand hand, net.minecraft.item.ItemStack stack) {
        return this.handle.control().setHeldItem(hand, stack);
    }

    public ActionResult interactBlock(net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.Direction side, Hand hand) {
        return this.handle.control().interactBlock(pos, side, hand);
    }

    public ActionResult mount(Entity vehicle) {
        return this.handle.control().mount(vehicle);
    }

    public ActionResult dismount() {
        return this.handle.control().dismount();
    }

    public ActionResult giveItem(net.minecraft.item.ItemStack stack) {
        return this.handle.control().giveItem(stack);
    }

    public ActionResult setHealth(double health) {
        return this.handle.control().setHealth(health);
    }

    public ActionResult setFoodLevel(int level) {
        return this.handle.control().setFoodLevel(level);
    }

    public ActionResult addExperience(int amount) {
        return this.handle.control().addExperience(amount);
    }

    public ActionResult executeCommand(String command) {
        return this.handle.control().executeCommand(command);
    }

    public ActionResult playSound(net.minecraft.sound.SoundEvent event) {
        return this.handle.control().playSound(event);
    }

    public ActionResult setGameMode(net.minecraft.world.GameMode mode) {
        return this.handle.control().setGameMode(mode);
    }

    public ActionResult sendChatMessage(String message) {
        return this.handle.control().sendChatMessage(message);
    }

    public ActionResult pathfindTo(net.minecraft.util.math.BlockPos target, double speed) {
        return this.handle.control().pathfindTo(target, speed);
    }

    public java.util.List<com.crpi.fakeplayer.control.ContainerInfo> getNearbyContainers(double radius) {
        return this.handle.control().getNearbyContainers(radius);
    }

    /** Executes an action immediately through the shared scheduler. */
    public ActionResult execute(Action action) {
        return CRPIFakePlayerMod.scheduler().runNow(action);
    }
}

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

    /** Executes an action immediately through the shared scheduler. */
    public ActionResult execute(Action action) {
        return CRPIFakePlayerMod.scheduler().runNow(action);
    }
}

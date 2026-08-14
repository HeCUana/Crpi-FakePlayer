package com.crpi.fakeplayer.control;

import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A continuous control task driven by the server tick. One task per fake
 * player at a time; starting a new task cancels the previous one.
 */
public abstract class ControlTask {
    private final FakePlayerHandle handle;
    private boolean finished;
    private ActionResult result = ActionResult.FAIL;

    protected ControlTask(FakePlayerHandle handle) {
        this.handle = handle;
    }

    public FakePlayerHandle handle() {
        return this.handle;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public ActionResult result() {
        return this.result;
    }

    protected void finish(ActionResult result) {
        this.result = result;
        this.finished = true;
    }

    /** Called once per server tick while the task is active. */
    public abstract void tick(long currentTick);

    /** Called when another task replaces this one or the player disconnects. */
    public void cancel() {
        this.finish(ActionResult.ABORT);
    }

    /** Helper: horizontal distance from the player's feet to a block centre. */
    protected static double horizontalDistance(FakePlayerHandle handle, Vec3d target) {
        double dx = target.x - handle.x();
        double dz = target.z - handle.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Helper: full 3D distance from the player's feet to a block centre. */
    protected static double distance(FakePlayerHandle handle, Vec3d target) {
        double dx = target.x - handle.x();
        double dy = target.y - handle.y();
        double dz = target.z - handle.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Block centre at feet height for a target block. */
    protected static Vec3d blockTarget(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
}

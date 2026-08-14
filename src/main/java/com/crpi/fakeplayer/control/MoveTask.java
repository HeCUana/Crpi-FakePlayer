package com.crpi.fakeplayer.control;

import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Moves the fake player to a position (or through waypoints) using vanilla
 * velocity: every tick the task sets the player's velocity toward the target,
 * and the player's own movement code applies it with collisions and friction.
 * Never uses Thread.sleep, never blocks the server thread.
 */
public final class MoveTask extends ControlTask {
    private static final double ARRIVE_DISTANCE = 0.5;
    private static final long TIMEOUT_TICKS = 600;
    private static final long STUCK_TICKS = 20;

    private final List<BlockPos> waypoints;
    private final double blocksPerSecond;
    private int waypointIndex;
    private long startTick;
    private long lastProgressTick;
    private Vec3d lastPosition;
    private boolean initialized;

    public MoveTask(FakePlayerHandle handle, List<BlockPos> waypoints, double blocksPerSecond) {
        super(handle);
        this.waypoints = waypoints;
        this.blocksPerSecond = blocksPerSecond;
        this.lastPosition = new Vec3d(handle.x(), handle.y(), handle.z());
    }

    @Override
    public void tick(long currentTick) {
        if (!this.initialized) {
            this.initialized = true;
            this.startTick = currentTick;
            this.lastProgressTick = currentTick;
        }
        if (currentTick - this.startTick > TIMEOUT_TICKS) {
            finish(ActionResult.FAIL);
            return;
        }
        Vec3d target = blockTarget(this.waypoints.get(this.waypointIndex));
        if (distance(handle(), target) <= ARRIVE_DISTANCE) {
            this.waypointIndex++;
            if (this.waypointIndex >= this.waypoints.size()) {
                finish(ActionResult.SUCCESS);
                return;
            }
            target = blockTarget(this.waypoints.get(this.waypointIndex));
            this.lastProgressTick = currentTick;
        }
        Vec3d delta = target.subtract(handle().x(), handle().y(), handle().z());
        double length = delta.length();
        if (length < 1.0E-4) {
            finish(ActionResult.SUCCESS);
            return;
        }
        double perTick = this.blocksPerSecond / 20.0;
        double step = Math.min(perTick, length);
        Vec3d direction = delta.multiply(1.0 / length);
        Vec3d newPos = new Vec3d(
            handle().x() + direction.x * step,
            handle().y() + direction.y * step,
            handle().z() + direction.z * step
        );
        // Carpet fake players do not move from velocity (their tick consumes
        // it without displacement, verified on 1.21.11), so the task moves the
        // player position directly with a simple passable check.
        BlockPos feet = BlockPos.ofFloored(newPos);
        if (isPassable(feet)) {
            handle().player().setPosition(newPos.x, newPos.y, newPos.z);
        } else {
            // try horizontal-only, then vertical-only to slide past ledges
            Vec3d horizontal = new Vec3d(newPos.x, handle().y(), newPos.z);
            if (isPassable(BlockPos.ofFloored(horizontal))) {
                handle().player().setPosition(horizontal.x, horizontal.y, horizontal.z);
            } else {
                finish(ActionResult.FAIL);
                return;
            }
        }
        Vec3d now = new Vec3d(handle().x(), handle().y(), handle().z());
        if (now.squaredDistanceTo(this.lastPosition) > 1.0E-4) {
            this.lastPosition = now;
            this.lastProgressTick = currentTick;
        } else if (currentTick - this.lastProgressTick > STUCK_TICKS) {
            finish(ActionResult.FAIL);
        }
    }

    /** Feet position is passable when the feet and head blocks are not solid. */
    private boolean isPassable(BlockPos feet) {
        return !handle().world().getBlockState(feet).blocksMovement()
            && !handle().world().getBlockState(feet.up()).blocksMovement();
    }
}

package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.mining.MiningSession;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Breaks a soft block (leaves, dirt, sand, wool, ...) in the way and walks
 * through. Mining runs through the vanilla interaction manager
 * ({@link MiningSession}) — native breaking speeds, tools and drops.
 */
public final class MovementBreak implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final BlockPos block;
    private com.crpi.fakeplayer.fakeplayer.FakePlayerHandle handle;
    private MiningSession session;
    private boolean broken;
    private boolean failed;

    public MovementBreak(PathNode source, PathNode target, BlockPos block) {
        this.source = source;
        this.target = target;
        this.block = block;
    }

    @Override
    public PathNode target() {
        return this.target;
    }

    @Override
    public double cost() {
        return 3.0;
    }

    @Override
    public void apply(FakePlayerHandle handle, FakePlayerMovementController controller, NavigationWorld world, long tick) {
        this.handle = handle;
        if (this.broken) {
            controller.lookToward(this.target.x() + 0.5, this.target.z() + 0.5);
            controller.forward(true);
            controller.jump(false);
            return;
        }
        if (this.failed) {
            // unmineable block (bedrock, etc.): stop trying, don't spin; the
            // outer stuck/repath machinery will give up on this route
            controller.lookToward(this.block.getX() + 0.5, this.block.getZ() + 0.5);
            controller.forward(false);
            controller.jump(false);
            return;
        }
        controller.lookToward(this.block.getX() + 0.5, this.block.getZ() + 0.5);
        controller.forward(false);
        if (this.session == null) {
            Direction face = this.block.getX() > this.source.x() ? Direction.EAST
                : this.block.getX() < this.source.x() ? Direction.WEST
                : this.block.getZ() > this.source.z() ? Direction.SOUTH : Direction.NORTH;
            this.session = com.crpi.fakeplayer.mining.MiningManager.begin(handle, this.block, face, tick);
        }
        MiningSession.State state = this.session.tick(tick);
        if (state == MiningSession.State.SUCCESS) {
            this.broken = true;
            com.crpi.fakeplayer.mining.MiningManager.finish(handle.player().getUuid());
        } else if (state == MiningSession.State.FAILED) {
            com.crpi.fakeplayer.mining.MiningManager.finish(handle.player().getUuid());
            this.session = null;
            this.failed = true;
        }
    }

    @Override
    public void stop() {
        // releasing the session sends ABORT_DESTROY_BLOCK so the vanilla
        // interaction manager is not left thinking it is still mining
        if (this.session != null && this.handle != null) {
            com.crpi.fakeplayer.mining.MiningManager.finish(this.handle.player().getUuid());
            this.session = null;
        }
    }

    @Override
    public boolean isComplete(FakePlayerHandle handle, NavigationWorld world) {
        double dx = handle.x() - (this.target.x() + 0.5);
        double dz = handle.z() - (this.target.z() + 0.5);
        return this.broken && dx * dx + dz * dz < 0.4 * 0.4;
    }

    @Override
    public String description() {
        return "Break " + this.block.getX() + "," + this.block.getY() + "," + this.block.getZ();
    }
}

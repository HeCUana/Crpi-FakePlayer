package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;

/**
 * Jump across a gap of 1-2 blocks (the gap floor is empty; the other side
 * is standable at the same height). Sprint + jump on ground; the vanilla
 * run-jump carries the player over.
 */
public final class MovementParkour implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final int dx;
    private final int dz;
    private final int span;
    private final boolean sprint;

    public MovementParkour(PathNode source, PathNode target, int dx, int dz, int span, boolean sprint) {
        this.source = source;
        this.target = target;
        this.dx = dx;
        this.dz = dz;
        this.span = span;
        this.sprint = sprint;
    }

    @Override
    public PathNode target() {
        return this.target;
    }

    @Override
    public double cost() {
        return this.span == 1 ? 1.5 : 1.8;
    }

    @Override
    public void apply(FakePlayerHandle handle, FakePlayerMovementController controller, NavigationWorld world, long tick) {
        controller.lookToward(this.target.x() + 0.5, this.target.z() + 0.5);
        controller.forward(true);
        controller.sprint(this.sprint);
        controller.jump(handle.player().isOnGround());
    }

    @Override
    public boolean isComplete(FakePlayerHandle handle, NavigationWorld world) {
        double dx = handle.x() - (this.target.x() + 0.5);
        double dz = handle.z() - (this.target.z() + 0.5);
        return handle.y() >= this.target.y() && dx * dx + dz * dz < 0.5 * 0.5;
    }

    @Override
    public String description() {
        return "Parkour " + (this.source.x() + this.dx * this.span) + "," + this.source.y() + ","
            + (this.source.z() + this.dz * this.span) + " span=" + this.span;
    }
}

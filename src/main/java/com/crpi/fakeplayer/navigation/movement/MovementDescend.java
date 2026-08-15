package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;

/**
 * One-block step down. Forward input only; gravity does the descent.
 */
public final class MovementDescend implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final int dx;
    private final int dz;

    public MovementDescend(PathNode source, PathNode target, int dx, int dz) {
        this.source = source;
        this.target = target;
        this.dx = dx;
        this.dz = dz;
    }

    @Override
    public PathNode target() {
        return this.target;
    }

    @Override
    public double cost() {
        return 1.0;
    }

    @Override
    public void apply(FakePlayerHandle handle, FakePlayerMovementController controller, NavigationWorld world, long tick) {
        controller.lookToward(this.target.x() + 0.5, this.target.z() + 0.5);
        controller.forward(true);
        controller.jump(false);
    }

    @Override
    public boolean isComplete(FakePlayerHandle handle, NavigationWorld world) {
        double dx = handle.x() - (this.target.x() + 0.5);
        double dz = handle.z() - (this.target.z() + 0.5);
        return handle.y() <= this.target.y() + 0.5 && dx * dx + dz * dz < 0.45 * 0.45;
    }

    @Override
    public String description() {
        return "Descend " + (this.source.x() + this.dx) + "," + (this.source.y() - 1) + "," + (this.source.z() + this.dz);
    }
}

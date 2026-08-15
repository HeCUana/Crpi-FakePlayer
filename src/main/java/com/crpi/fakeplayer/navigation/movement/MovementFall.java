package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;

/**
 * A safe multi-block drop (2..maxFallDistance blocks down, one block
 * horizontally). Forward input only; gravity does the fall. The provider
 * limits the drop to the profile's max fall distance so no unsafe fall is
 * ever planned.
 */
public final class MovementFall implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final int dx;
    private final int dz;
    private final int drop;

    public MovementFall(PathNode source, PathNode target, int dx, int dz, int drop) {
        this.source = source;
        this.target = target;
        this.dx = dx;
        this.dz = dz;
        this.drop = drop;
    }

    @Override
    public PathNode target() {
        return this.target;
    }

    @Override
    public double cost() {
        return 1.0 + 0.2 * this.drop;
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
        return "Fall " + (this.source.x() + this.dx) + "," + (this.source.y() - this.drop) + "," + (this.source.z() + this.dz) + " drop=" + this.drop;
    }
}

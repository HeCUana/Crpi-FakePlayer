package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;

/**
 * One-block horizontal step (N/S/E/W). Applied via the Carpet action pack
 * forward input; Minecraft physics performs the actual walk.
 */
public final class MovementTraverse implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final int dx;
    private final int dz;

    public MovementTraverse(PathNode source, PathNode target, int dx, int dz) {
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
        return dx * dx + dz * dz < 0.35 * 0.35;
    }

    @Override
    public String description() {
        return "Traverse " + (this.source.x() + this.dx) + "," + this.source.y() + "," + (this.source.z() + this.dz);
    }
}

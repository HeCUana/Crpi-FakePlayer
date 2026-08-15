package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;

/**
 * Diagonal step (NE/NW/SE/SW). Forward input toward the diagonal target;
 * vanilla physics performs the movement. The provider guarantees no
 * corner-cutting (both adjacent orthogonal positions must be passable).
 */
public final class MovementDiagonal implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final int dx;
    private final int dz;

    public MovementDiagonal(PathNode source, PathNode target, int dx, int dz) {
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
        return 1.414;
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
        return dx * dx + dz * dz < 0.4 * 0.4;
    }

    @Override
    public String description() {
        return "Diagonal " + (this.source.x() + this.dx) + "," + this.source.y() + "," + (this.source.z() + this.dz);
    }
}

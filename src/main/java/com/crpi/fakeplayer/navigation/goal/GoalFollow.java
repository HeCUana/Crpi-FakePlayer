package com.crpi.fakeplayer.navigation.goal;

import com.crpi.fakeplayer.navigation.path.PathNode;
import net.minecraft.entity.Entity;

/**
 * Follows a living entity. The goal position is the entity's current block
 * position, so it moves; the manager re-plans when the entity drifts. The
 * goal is satisfied while within {@code distance} of the entity and the
 * entity has been still for a while.
 */
public final class GoalFollow implements Goal {
    private final Entity target;
    private final int distance;

    public GoalFollow(Entity target, int distance) {
        this.target = target;
        this.distance = distance;
    }

    public Entity target() {
        return this.target;
    }

    public int distance() {
        return this.distance;
    }

    @Override
    public boolean isInGoal(PathNode node) {
        if (!this.target.isAlive()) {
            return false;
        }
        double dx = this.target.getX() - (node.x() + 0.5);
        double dz = this.target.getZ() - (node.z() + 0.5);
        double reach = this.distance + 0.5;
        return dx * dx + dz * dz <= reach * reach
            && Math.abs(this.target.getY() - node.y()) <= 1;
    }

    /** Exact check against the entity's current position (not block coords). */
    public boolean isReached(double x, double y, double z) {
        if (!this.target.isAlive()) {
            return false;
        }
        double dx = this.target.getX() - x;
        double dz = this.target.getZ() - z;
        double reach = this.distance + 0.5;
        return dx * dx + dz * dz <= reach * reach
            && Math.abs(this.target.getY() - y) <= 1;
    }

    @Override
    public double heuristic(PathNode node) {
        if (!this.target.isAlive()) {
            return 0;
        }
        double dx = this.target.getX() - node.x();
        double dy = this.target.getY() - node.y();
        double dz = this.target.getZ() - node.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return "GoalFollow{" + this.target.getName().getString()
            + " at=" + this.target.getBlockPos().toShortString() + " d=" + this.distance + "}";
    }
}

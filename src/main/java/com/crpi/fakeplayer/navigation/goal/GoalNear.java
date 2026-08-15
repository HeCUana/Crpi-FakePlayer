package com.crpi.fakeplayer.navigation.goal;

import com.crpi.fakeplayer.navigation.path.PathNode;
import net.minecraft.util.math.BlockPos;

/**
 * Get within a radius of a block position (any Y counts; XZ distance only
 * when {@code xzOnly} is true).
 */
public final class GoalNear implements Goal {
    private final int x;
    private final int y;
    private final int z;
    private final int radius;
    private final boolean xzOnly;

    public GoalNear(BlockPos pos, int radius) {
        this(pos, radius, false);
    }

    public GoalNear(BlockPos pos, int radius, boolean xzOnly) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.radius = radius;
        this.xzOnly = xzOnly;
    }

    @Override
    public boolean isInGoal(PathNode node) {
        int dx = node.x() - this.x;
        int dy = node.y() - this.y;
        int dz = node.z() - this.z;
        if (this.xzOnly) {
            return dx * dx + dz * dz <= this.radius * this.radius;
        }
        return dx * dx + dy * dy + dz * dz <= this.radius * this.radius;
    }

    @Override
    public double heuristic(PathNode node) {
        int dx = node.x() - this.x;
        int dy = node.y() - this.y;
        int dz = node.z() - this.z;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.max(0, d - this.radius);
    }

    @Override
    public String toString() {
        return "GoalNear{" + this.x + "," + this.y + "," + this.z + " r=" + this.radius + "}";
    }
}

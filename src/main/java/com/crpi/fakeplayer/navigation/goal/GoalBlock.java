package com.crpi.fakeplayer.navigation.goal;

import com.crpi.fakeplayer.navigation.path.PathNode;
import net.minecraft.util.math.BlockPos;

/**
 * Reach a specific block position (feet at the target).
 */
public final class GoalBlock implements Goal {
    private final int x;
    private final int y;
    private final int z;

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isInGoal(PathNode node) {
        return node.x() == this.x && node.y() == this.y && node.z() == this.z;
    }

    @Override
    public double heuristic(PathNode node) {
        int dx = node.x() - this.x;
        int dy = node.y() - this.y;
        int dz = node.z() - this.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return "GoalBlock{" + this.x + "," + this.y + "," + this.z + "}";
    }
}

package com.crpi.fakeplayer.navigation.cost;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * Extra traversal costs for the pathfinder. Base movement costs come from
 * the Movement implementations; this model adds danger/terrain penalties
 * around a node so paths prefer safe routes.
 */
public final class CostModel {
    private final Map<Block, Double> blockCosts = new HashMap<>();

    public CostModel() {
        this.blockCosts.put(Blocks.LAVA, 100000.0);
        this.blockCosts.put(Blocks.FIRE, 1000.0);
        this.blockCosts.put(Blocks.CACTUS, 500.0);
    }

    public void setBlockCost(Block block, double cost) {
        this.blockCosts.put(block, cost);
    }

    public double blockCost(Block block) {
        return this.blockCosts.getOrDefault(block, 0.0);
    }

    /**
     * Extra cost for landing on / walking past {@code pos}: the block under
     * the feet plus any dangerous neighbour in a 3x3 ring.
     */
    public double extraCost(com.crpi.fakeplayer.navigation.world.NavigationWorld world, BlockPos feet) {
        double cost = this.blockCost(world.state(feet.down()).getBlock());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos neighbour = feet.add(dx, 0, dz);
                if (world.isDangerous(neighbour)) {
                    cost += this.blockCost(world.state(neighbour).getBlock()) * 0.1;
                }
            }
        }
        return cost;
    }
}

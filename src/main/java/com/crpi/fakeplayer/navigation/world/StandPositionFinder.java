package com.crpi.fakeplayer.navigation.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;

/**
 * Finds every standable position around a target block: feet must have a
 * solid block below, body and head passable, and the position must be in a
 * loaded chunk. Basis for interaction-position finding (Phase 6).
 */
public final class StandPositionFinder {
    private final NavigationWorld world;

    public StandPositionFinder(NavigationWorld world) {
        this.world = world;
    }

    /**
     * @param target         the block to stand next to
     * @param horizontalMax  search radius in X/Z (1 = 3x3 ring)
     * @param verticalMax    max Y offset above/below the target
     * @return standable positions sorted by distance to the target centre
     */
    public List<StandPosition> find(BlockPos target, int horizontalMax, int verticalMax) {
        List<StandPosition> positions = new ArrayList<>();
        for (int dx = -horizontalMax; dx <= horizontalMax; dx++) {
            for (int dz = -horizontalMax; dz <= horizontalMax; dz++) {
                for (int dy = -verticalMax; dy <= verticalMax; dy++) {
                    BlockPos pos = target.add(dx, dy, dz);
                    if (!this.world.isLoaded(pos)) {
                        continue;
                    }
                    if (this.world.canStandAt(pos) && !this.world.isDangerous(pos.down())) {
                        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        positions.add(new StandPosition(pos.toImmutable(), d, dy));
                    }
                }
            }
        }
        return StandPosition.sort(positions);
    }
}

package com.crpi.fakeplayer.navigation.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.BlockPos;

/**
 * A position a player can stand at, relative to a target block.
 */
public record StandPosition(BlockPos pos, double distance, int relativeY) {

    /** Sorted by distance ascending. */
    public static List<StandPosition> sort(List<StandPosition> positions) {
        List<StandPosition> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparingDouble(StandPosition::distance));
        return sorted;
    }
}

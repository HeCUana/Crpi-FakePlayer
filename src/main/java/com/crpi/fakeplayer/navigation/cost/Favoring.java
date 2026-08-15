package com.crpi.fakeplayer.navigation.cost;

import com.crpi.fakeplayer.navigation.path.PathNode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;

/**
 * Positional cost adjustments. Failed/stuck positions get penalised so the
 * next path calculation picks a different route instead of repeating the
 * same failure.
 */
public final class Favoring {
    private final Map<Long, Double> favors = new HashMap<>();

    /** Adds a cost delta for a position (negative = favour, positive = avoid). */
    public void favor(BlockPos pos, double costDelta) {
        this.favors.merge(hash(pos), costDelta, Double::sum);
    }

    public double favorFor(PathNode node) {
        return this.favors.getOrDefault(node.hash(), 0.0);
    }

    public void clear() {
        this.favors.clear();
    }

    public int size() {
        return this.favors.size();
    }

    private static long hash(BlockPos pos) {
        long h = 1125899906842597L;
        h = 31 * h + pos.getX();
        h = 31 * h + pos.getY();
        h = 31 * h + pos.getZ();
        return h;
    }
}

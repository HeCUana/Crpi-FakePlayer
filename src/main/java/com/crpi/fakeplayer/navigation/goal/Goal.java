package com.crpi.fakeplayer.navigation.goal;

import com.crpi.fakeplayer.navigation.path.PathNode;

/**
 * A navigation goal: knows when a node satisfies it and provides the A*
 * heuristic.
 */
public interface Goal {
    boolean isInGoal(PathNode node);

    double heuristic(PathNode node);

    default boolean isInGoal(int x, int y, int z) {
        return isInGoal(new PathNode(x, y, z, 0, 0, null, null));
    }
}

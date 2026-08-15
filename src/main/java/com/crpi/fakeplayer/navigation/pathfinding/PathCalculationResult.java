package com.crpi.fakeplayer.navigation.pathfinding;

import com.crpi.fakeplayer.navigation.path.Path;

/**
 * Result of a path calculation. Never null: failures carry a reason.
 */
public record PathCalculationResult(Path path, Status status) {

    public enum Status {
        COMPLETE,
        PARTIAL,            // budget/node limit hit; path reaches the best node found
        FAILED              // no path exists (goal unreachable / world unavailable)
    }

    public boolean hasPath() {
        return this.path != null && this.path.length() > 0;
    }
}

package com.crpi.fakeplayer.navigation.path;

import com.crpi.fakeplayer.navigation.goal.Goal;
import com.crpi.fakeplayer.navigation.movement.Movement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A computed path: ordered list of movements from start to goal.
 */
public final class Path {
    private final List<Movement> movements;
    private final Goal goal;
    private final double totalCost;
    private final long createdTick;
    private int currentIndex;

    public Path(List<Movement> movements, Goal goal, double totalCost, long createdTick) {
        this.movements = new ArrayList<>(movements);
        this.goal = goal;
        this.totalCost = totalCost;
        this.createdTick = createdTick;
    }

    public List<Movement> movements() {
        return Collections.unmodifiableList(this.movements);
    }

    public Goal goal() {
        return this.goal;
    }

    public double totalCost() {
        return this.totalCost;
    }

    public long createdTick() {
        return this.createdTick;
    }

    public Movement currentMovement() {
        return this.currentIndex < this.movements.size() ? this.movements.get(this.currentIndex) : null;
    }

    public Movement nextMovement() {
        int next = this.currentIndex + 1;
        return next < this.movements.size() ? this.movements.get(next) : null;
    }

    public void advance() {
        this.currentIndex++;
    }

    public boolean isFinished() {
        return this.currentIndex >= this.movements.size();
    }

    public int currentIndex() {
        return this.currentIndex;
    }

    public int length() {
        return this.movements.size();
    }
}

package com.crpi.fakeplayer.navigation.path;

import com.crpi.fakeplayer.navigation.movement.Movement;

/**
 * A* node: a position with costs, parent linkage and the movement that
 * reached it. Positions are FEET coordinates.
 */
public final class PathNode {
    private final int x;
    private final int y;
    private final int z;
    private double gCost;
    private double fCost;
    private PathNode parent;
    private Movement cameFromMovement;
    private boolean closed;

    public PathNode(int x, int y, int z, double gCost, double fCost, PathNode parent, Movement cameFromMovement) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.gCost = gCost;
        this.fCost = fCost;
        this.parent = parent;
        this.cameFromMovement = cameFromMovement;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

    public double gCost() {
        return this.gCost;
    }

    public void gCost(double gCost) {
        this.gCost = gCost;
    }

    public double fCost() {
        return this.fCost;
    }

    public void fCost(double fCost) {
        this.fCost = fCost;
    }

    public PathNode parent() {
        return this.parent;
    }

    public void parent(PathNode parent) {
        this.parent = parent;
    }

    public Movement cameFromMovement() {
        return this.cameFromMovement;
    }

    public void cameFromMovement(Movement movement) {
        this.cameFromMovement = movement;
    }

    public boolean closed() {
        return this.closed;
    }

    public void closed(boolean closed) {
        this.closed = closed;
    }

    public long hash() {
        // mixing hash: no bit-packing overflow, fine for the node counts used here
        long h = 1125899906842597L;
        h = 31 * h + this.x;
        h = 31 * h + this.y;
        h = 31 * h + this.z;
        return h;
    }

    @Override
    public String toString() {
        return "PathNode{" + this.x + "," + this.y + "," + this.z + "}";
    }
}

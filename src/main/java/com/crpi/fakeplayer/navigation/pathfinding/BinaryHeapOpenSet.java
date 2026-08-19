package com.crpi.fakeplayer.navigation.pathfinding;

import com.crpi.fakeplayer.navigation.path.PathNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Binary-heap open set keyed by fCost (with node hash de-duplication).
 */
public final class BinaryHeapOpenSet {
    private final PriorityQueue<PathNode> heap = new PriorityQueue<>(
        Comparator.comparingDouble((PathNode n) -> n.fCost()).thenComparingLong(PathNode::hash));
    private final Map<Long, PathNode> index = new HashMap<>();

    public void add(PathNode node) {
        Long hash = node.hash();
        PathNode existing = this.index.get(hash);
        if (existing == null) {
            this.index.put(hash, node);
            this.heap.add(node);
        } else if (node.gCost() < existing.gCost()) {
            existing.parent(node.parent());
            existing.cameFromMovement(node.cameFromMovement());
            // the heap orders by fCost: update the key too, or the node sits at
            // the wrong position and expanded neighbours carry inflated costs
            existing.gCost(node.gCost());
            existing.fCost(node.fCost());
            this.heap.remove(existing);
            this.heap.add(existing);
        }
    }

    public PathNode poll() {
        PathNode node = this.heap.poll();
        if (node != null) {
            this.index.remove(node.hash());
        }
        return node;
    }

    public boolean isEmpty() {
        return this.heap.isEmpty();
    }

    public int size() {
        return this.heap.size();
    }
}

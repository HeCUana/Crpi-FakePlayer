package com.crpi.fakeplayer.navigation.pathfinding;

import com.crpi.fakeplayer.navigation.cost.CostModel;
import com.crpi.fakeplayer.navigation.cost.Favoring;
import com.crpi.fakeplayer.navigation.goal.Goal;
import com.crpi.fakeplayer.navigation.movement.Movement;
import com.crpi.fakeplayer.navigation.movement.MovementProvider;
import com.crpi.fakeplayer.navigation.path.Path;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronous A* over the movement graph. Neighbours come exclusively from
 * the {@link MovementProvider}, never from raw adjacent positions.
 *
 * <p>Budget-controlled: max nodes, max search radius and a nanosecond time
 * budget. Exceeding them returns PARTIAL with the best path found so far
 * (execution continues and re-plans), never blocks or loops forever.
 */
public final class AStarPathFinder {
    private final MovementProvider movementProvider = new MovementProvider();
    private final Map<Long, PathNode> bestNodes = new HashMap<>();
    private final Map<Long, Double> gCosts = new HashMap<>();
    private final CostModel costModel;
    private final Favoring favoring;

    public AStarPathFinder(CostModel costModel, Favoring favoring) {
        this.costModel = costModel;
        this.favoring = favoring;
    }

    /**
     * @param world       world view (synchronous in Phase 1)
     * @param start       start node (feet position)
     * @param goal        goal
     * @param maxNodes    node expansion limit
     * @param maxRadius   maximum Chebyshev distance from start
     * @param budgetNanos time budget
     */
    public PathCalculationResult findPath(
        NavigationWorld world,
        PathNode start,
        Goal goal,
        int maxNodes,
        int maxRadius,
        long budgetNanos
    ) {
        long deadline = System.nanoTime() + budgetNanos;
        this.bestNodes.clear();
        this.gCosts.clear();

        BinaryHeapOpenSet openSet = new BinaryHeapOpenSet();
        start.fCost(goal.heuristic(start));
        openSet.add(start);
        this.gCosts.put(start.hash(), start.gCost());
        this.bestNodes.put(start.hash(), start);

        int expanded = 0;
        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            if (current.closed()) {
                continue;
            }
            current.closed(true);
            expanded++;
            if (goal.isInGoal(current)) {
                return new PathCalculationResult(buildPath(current, goal, world.world().getServer().getTicks()),
                    PathCalculationResult.Status.COMPLETE);
            }
            if (expanded >= maxNodes || System.nanoTime() > deadline) {
                PathNode best = bestNodeNearGoal(goal);
                if (best != null && best != start) {
                    return new PathCalculationResult(buildPath(best, goal, world.world().getServer().getTicks()),
                        PathCalculationResult.Status.PARTIAL);
                }
                return new PathCalculationResult(null, PathCalculationResult.Status.FAILED);
            }
            for (Movement movement : this.movementProvider.generate(world, current)) {
                if (expanded >= maxNodes || System.nanoTime() > deadline) {
                    PathNode best = bestNodeNearGoal(goal);
                    if (best != null && best != start) {
                        return new PathCalculationResult(buildPath(best, goal, world.world().getServer().getTicks()),
                            PathCalculationResult.Status.PARTIAL);
                    }
                    return new PathCalculationResult(null, PathCalculationResult.Status.FAILED);
                }
                PathNode target = movement.target();
                if (target.closed()) {
                    continue;
                }
                if (Math.abs(target.x() - start.x()) > maxRadius
                    || Math.abs(target.z() - start.z()) > maxRadius) {
                    continue;
                }
                double tentativeG = current.gCost() + movement.cost()
                    + this.costModel.extraCost(world, new net.minecraft.util.math.BlockPos(target.x(), target.y(), target.z()))
                    + this.favoring.favorFor(target);
                Double knownG = this.gCosts.get(target.hash());
                if (knownG != null && tentativeG >= knownG) {
                    continue;
                }
                target.parent(current);
                target.cameFromMovement(movement);
                target.gCost(tentativeG);
                target.fCost(tentativeG + goal.heuristic(target));
                this.gCosts.put(target.hash(), tentativeG);
                this.bestNodes.put(target.hash(), target);
                openSet.add(target);
            }
        }
        PathNode best = bestNodeNearGoal(goal);
        if (best != null && best != start) {
            return new PathCalculationResult(buildPath(best, goal, world.world().getServer().getTicks()),
                PathCalculationResult.Status.PARTIAL);
        }
        return new PathCalculationResult(null, PathCalculationResult.Status.FAILED);
    }

    private PathNode bestNodeNearGoal(Goal goal) {
        PathNode best = null;
        double bestF = Double.MAX_VALUE;
        for (PathNode node : this.bestNodes.values()) {
            double f = node.gCost() + goal.heuristic(node);
            if (f < bestF) {
                bestF = f;
                best = node;
            }
        }
        return best;
    }

    private Path buildPath(PathNode end, Goal goal, long tick) {
        List<Movement> movements = new ArrayList<>();
        double cost = 0;
        PathNode cursor = end;
        while (cursor.parent() != null && cursor.cameFromMovement() != null) {
            movements.add(0, cursor.cameFromMovement());
            cost += cursor.cameFromMovement().cost();
            cursor = cursor.parent();
        }
        return new Path(movements, goal, cost, tick);
    }
}

package com.crpi.fakeplayer.navigation.goal;

import com.crpi.fakeplayer.navigation.path.PathNode;
import java.util.Arrays;
import java.util.List;

/**
 * Combines sub-goals. ANY_OF completes when the first sub-goal is reached;
 * ALL_OF completes when every sub-goal is reached. The heuristic is the
 * min (ANY) or max (ALL) of the sub-heuristics.
 */
public final class GoalComposite implements Goal {
    public enum Mode {
        ANY_OF, ALL_OF
    }

    private final List<Goal> goals;
    private final Mode mode;

    public GoalComposite(Mode mode, Goal... goals) {
        this.goals = Arrays.asList(goals);
        this.mode = mode;
    }

    public List<Goal> goals() {
        return this.goals;
    }

    public Mode mode() {
        return this.mode;
    }

    @Override
    public boolean isInGoal(PathNode node) {
        if (this.mode == Mode.ANY_OF) {
            for (Goal goal : this.goals) {
                if (goal.isInGoal(node)) {
                    return true;
                }
            }
            return false;
        }
        for (Goal goal : this.goals) {
            if (!goal.isInGoal(node)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double heuristic(PathNode node) {
        double best = this.mode == Mode.ANY_OF ? Double.MAX_VALUE : 0.0;
        for (Goal goal : this.goals) {
            double h = goal.heuristic(node);
            best = this.mode == Mode.ANY_OF ? Math.min(best, h) : Math.max(best, h);
        }
        return best;
    }

    @Override
    public String toString() {
        return "GoalComposite{" + this.mode + " " + this.goals + "}";
    }
}

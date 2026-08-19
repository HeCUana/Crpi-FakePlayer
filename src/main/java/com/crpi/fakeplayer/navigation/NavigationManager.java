package com.crpi.fakeplayer.navigation;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.cost.CostModel;
import com.crpi.fakeplayer.navigation.cost.Favoring;
import com.crpi.fakeplayer.navigation.executor.PathExecutor;
import com.crpi.fakeplayer.navigation.goal.Goal;
import com.crpi.fakeplayer.navigation.goal.GoalBlock;
import com.crpi.fakeplayer.navigation.goal.GoalNear;
import com.crpi.fakeplayer.navigation.movement.Movement;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.Path;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.pathfinding.AStarPathFinder;
import com.crpi.fakeplayer.navigation.pathfinding.PathCalculationResult;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-fake-player navigation entry point. Coordinates goal → A* → path
 * execution → completion/repath. All world interaction happens on the
 * server thread via the tick driver.
 */
public final class NavigationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CRPI-FakePlayer");
    private static final int MAX_NODES = 1000;
    private static final int MAX_REPATHES = 3;
    private static final int REPATH_COOLDOWN_TICKS = 20;

    public enum NavigationStatus {
        IDLE, CALCULATING, RUNNING, SUCCESS, FAILED, STUCK, CANCELLED
    }

    private final FakePlayerHandle handle;
    private final NavigationWorld world;
    private final FakePlayerMovementController controller;
    private final PathExecutor executor;
    private final CostModel costModel = new CostModel();
    private final Favoring favoring = new Favoring();
    private final AStarPathFinder pathFinder = new AStarPathFinder(this.costModel, this.favoring);

    private NavigationProfile profile = NavigationProfile.NORMAL;
    private Goal goal;
    private NavigationStatus status = NavigationStatus.IDLE;
    private int repaths;
    private long lastRepathTick;
    private boolean invalidated;
    private BlockPos lastFollowPos;

    public NavigationManager(FakePlayerHandle handle) {
        this.handle = handle;
        this.world = new NavigationWorld(handle.world());
        this.controller = new FakePlayerMovementController(handle);
        this.executor = new PathExecutor(handle, this.controller, this.world);
    }

    public FakePlayerHandle handle() {
        return this.handle;
    }

    public NavigationProfile profile() {
        return this.profile;
    }

    public void setProfile(NavigationProfile profile) {
        this.profile = profile;
        this.world.setProfile(profile);
    }

    public CostModel costModel() {
        return this.costModel;
    }

    public Favoring favoring() {
        return this.favoring;
    }

    public Goal goal() {
        return this.goal;
    }

    public Path currentPath() {
        return this.executor.path();
    }

    public NavigationStatus status() {
        return this.status;
    }

    public int repaths() {
        return this.repaths;
    }

    public boolean isNavigating() {
        return this.status == NavigationStatus.CALCULATING || this.status == NavigationStatus.RUNNING;
    }

    public boolean isFinished() {
        return this.status == NavigationStatus.SUCCESS || this.status == NavigationStatus.FAILED
            || this.status == NavigationStatus.CANCELLED;
    }

    // ---- public API ----

    public boolean gotoBlock(BlockPos target) {
        return start(new GoalBlock(target));
    }

    public boolean gotoNear(BlockPos target, int radius) {
        return start(new GoalNear(target, radius));
    }

    /** Navigates to whichever of the given targets is reachable first. */
    public boolean gotoAny(BlockPos... targets) {
        Goal[] goals = new Goal[targets.length];
        for (int i = 0; i < targets.length; i++) {
            goals[i] = new GoalBlock(targets[i]);
        }
        return start(new com.crpi.fakeplayer.navigation.goal.GoalComposite(
            com.crpi.fakeplayer.navigation.goal.GoalComposite.Mode.ANY_OF, goals));
    }

    public void follow(Entity entity) {
        follow(entity, 2);
    }

    /** Follows an entity, keeping within {@code distance} blocks. */
    public void follow(Entity entity, int distance) {
        start(new com.crpi.fakeplayer.navigation.goal.GoalFollow(entity, distance));
    }

    /**
     * Executes an explicit waypoint path (no A*): each waypoint becomes a
     * traverse movement. Movement stays physics-native.
     */
    public boolean followPath(java.util.List<BlockPos> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return false;
        }
        this.executor.stop();
        this.repaths = 0;
        this.invalidated = false;
        this.favoring.clear();
        java.util.List<com.crpi.fakeplayer.navigation.movement.Movement> movements = new java.util.ArrayList<>();
        BlockPos current = this.handle.player().getBlockPos();
        for (BlockPos next : waypoints) {
            int dx = next.getX() - current.getX();
            int dz = next.getZ() - current.getZ();
            if (next.getY() != current.getY()) {
                return false; // Phase 5: same-height waypoints only
            }
            PathNode from = new PathNode(current.getX(), current.getY(), current.getZ(), 0, 0, null, null);
            PathNode to = new PathNode(next.getX(), next.getY(), next.getZ(), 0, 0, null, null);
            movements.add(new com.crpi.fakeplayer.navigation.movement.MovementTraverse(from, to, dx, dz));
            current = next;
        }
        this.goal = null;
        long tick = this.handle.world().getServer().getTicks();
        this.executor.start(new Path(movements, null, movements.size(), tick));
        this.status = NavigationStatus.RUNNING;
        return true;
    }

    public void stop() {
        this.executor.stop();
        this.goal = null;
        this.status = NavigationStatus.CANCELLED;
    }

    public void pause() {
        // release the in-flight movement's resources (e.g. mining session) too
        this.executor.stop();
        if (this.status == NavigationStatus.RUNNING || this.status == NavigationStatus.CALCULATING) {
            this.status = NavigationStatus.CANCELLED;
        }
    }

    public void resume() {
        if (this.goal != null) {
            this.repath();
        }
    }

    public boolean repath() {
        if (this.goal == null) {
            return false;
        }
        long now = this.handle.world().getServer().getTicks();
        if (now - this.lastRepathTick < REPATH_COOLDOWN_TICKS) {
            return false;
        }
        this.lastRepathTick = now;
        if (this.repaths >= MAX_REPATHES) {
            this.executor.stop();
            this.status = NavigationStatus.FAILED;
            return false;
        }
        this.repaths++;
        this.status = NavigationStatus.CALCULATING;
        PathCalculationResult result = calculate();
        if (result.hasPath()) {
            this.executor.start(result.path());
            this.status = NavigationStatus.RUNNING;
            return true;
        }
        this.executor.stop();
        this.status = NavigationStatus.FAILED;
        return false;
    }

    /** Drives navigation once per server tick. */
    public void tick() {
        // continuous following: the goal stays alive even after a previous
        // SUCCESS — if the target wanders off, resume the chase
        if (this.goal instanceof com.crpi.fakeplayer.navigation.goal.GoalFollow followGoal) {
            if (!followGoal.target().isAlive()) {
                if (this.status != NavigationStatus.FAILED) {
                    this.executor.stop();
                    this.status = NavigationStatus.FAILED;
                }
                return;
            }
            if (followGoal.isReached(this.handle.x(), this.handle.y(), this.handle.z())) {
                if (this.status == NavigationStatus.RUNNING || this.status == NavigationStatus.CALCULATING) {
                    this.executor.stop();
                    this.status = NavigationStatus.SUCCESS;
                    LOGGER.info("navigation followed player={} target={} repaths={}",
                        this.handle.name(), followGoal.target().getName().getString(), this.repaths);
                }
                return;
            }
            BlockPos targetPos = followGoal.target().getBlockPos();
            if (this.status == NavigationStatus.SUCCESS
                || this.lastFollowPos == null
                || Math.abs(targetPos.getX() - this.lastFollowPos.getX()) > 1
                || Math.abs(targetPos.getY() - this.lastFollowPos.getY()) > 1
                || Math.abs(targetPos.getZ() - this.lastFollowPos.getZ()) > 1) {
                this.lastFollowPos = targetPos.toImmutable();
                this.executor.stop();
                // following is a continuous task: re-planning is expected and
                // must not exhaust the failure budget
                this.repaths = 0;
                this.repath();
                return;
            }
            // target steady: fall through and keep executing the current path
        }
        if (this.status != NavigationStatus.RUNNING) {
            return;
        }
        if (!this.handle.isOnline()) {
            this.executor.stop();
            this.status = NavigationStatus.FAILED;
            return;
        }
        // the world changed under the planned path: re-plan with a penalty on
        // the current position so a different route is chosen
        if (!this.invalidated && this.executor.isCurrentMovementBlocked()) {
            this.invalidated = true;
            this.favoring.favor(this.handle.player().getBlockPos(), 1000.0);
            LOGGER.info("navigation path invalidated player={} at={} favor size={}",
                this.handle.name(), this.handle.player().getBlockPos(), this.favoring.size());
            this.executor.stop();
            this.repath();
            return;
        }
        if (this.executor.isStuck()) {
            this.status = NavigationStatus.STUCK;
            this.favoring.favor(this.handle.player().getBlockPos(), 500.0);
            if (!this.repath()) {
                this.status = NavigationStatus.FAILED;
            }
            return;
        }
        boolean finished = this.executor.tick();
        if (finished) {
            this.executor.stop();
            this.status = NavigationStatus.SUCCESS;
            LOGGER.info("navigation finished player={} goal={} repaths={}",
                this.handle.name(), this.goal, this.repaths);
        }
    }

    private boolean start(Goal newGoal) {
        this.executor.stop();
        this.goal = newGoal;
        this.repaths = 0;
        this.invalidated = false;
        this.favoring.clear();
        this.lastFollowPos = null;
        BlockPos startPos = this.handle.player().getBlockPos();
        boolean alreadyThere = newGoal instanceof com.crpi.fakeplayer.navigation.goal.GoalFollow followGoal
            ? followGoal.isReached(this.handle.x(), this.handle.y(), this.handle.z())
            : newGoal.isInGoal(startPos.getX(), startPos.getY(), startPos.getZ());
        if (alreadyThere) {
            // already at the goal
            this.status = NavigationStatus.SUCCESS;
            return true;
        }
        this.status = NavigationStatus.CALCULATING;
        PathCalculationResult result = calculate();
        if (result.hasPath()) {
            this.executor.start(result.path());
            this.status = NavigationStatus.RUNNING;
            return true;
        }
        this.status = NavigationStatus.FAILED;
        return false;
    }

    private PathCalculationResult calculate() {
        BlockPos startPos = this.handle.player().getBlockPos();
        PathNode start = new PathNode(startPos.getX(), startPos.getY(), startPos.getZ(), 0, 0, null, null);
        return this.pathFinder.findPath(
            this.world,
            start,
            this.goal,
            MAX_NODES,
            this.profile.maxSearchRadius,
            this.profile.maxCalculationBudgetNanos
        );
    }
}

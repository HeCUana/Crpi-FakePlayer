package com.crpi.fakeplayer.navigation.executor;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.Movement;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.Path;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;

/**
 * Executes a {@link Path} movement by movement: applies the current
 * movement's inputs every tick, waits for completion, then advances.
 * Movement itself only comes from Minecraft physics — no teleporting.
 */
public final class PathExecutor {
    private static final int STUCK_TICKS = 40;
    private static final double STUCK_DISTANCE = 0.02;

    private final FakePlayerHandle handle;
    private final FakePlayerMovementController controller;
    private final NavigationWorld world;
    private Path path;
    private long lastProgressTick;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean stuck;
    private long stuckTicks;

    public PathExecutor(FakePlayerHandle handle, FakePlayerMovementController controller, NavigationWorld world) {
        this.handle = handle;
        this.controller = controller;
        this.world = world;
    }

    public void start(Path newPath) {
        this.path = newPath;
        this.lastX = this.handle.x();
        this.lastY = this.handle.y();
        this.lastZ = this.handle.z();
        this.lastProgressTick = this.world.world().getServer().getTicks();
        this.stuck = false;
        this.stuckTicks = 0;
    }

    public Path path() {
        return this.path;
    }

    public boolean isStuck() {
        return this.stuck;
    }

    public long stuckTicks() {
        return this.stuckTicks;
    }

    /** Drives one tick. Returns true when the whole path finished. */
    public boolean tick() {
        if (this.path == null || this.path.isFinished()) {
            return true;
        }
        Movement current = this.path.currentMovement();
        if (current == null) {
            return true;
        }
        long tick = this.world.world().getServer().getTicks();
        current.apply(this.handle, this.controller, this.world, tick);

        double dx = this.handle.x() - this.lastX;
        double dy = this.handle.y() - this.lastY;
        double dz = this.handle.z() - this.lastZ;
        if (dx * dx + dy * dy + dz * dz > STUCK_DISTANCE * STUCK_DISTANCE) {
            this.lastX = this.handle.x();
            this.lastY = this.handle.y();
            this.lastZ = this.handle.z();
            this.lastProgressTick = tick;
        }
        long sinceProgress = tick - this.lastProgressTick;
        this.stuck = sinceProgress > STUCK_TICKS;
        this.stuckTicks = Math.max(0, sinceProgress - STUCK_TICKS);

        if (current.isComplete(this.handle, this.world)) {
            this.path.advance();
        }
        return this.path.isFinished();
    }

    public void stop() {
        this.controller.stop();
        this.path = null;
    }
}

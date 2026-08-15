package com.crpi.fakeplayer.navigation.movement.controller;

import carpet.fakes.ServerPlayerInterface;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.util.math.MathHelper;

/**
 * Input bridge for the navigation engine. Wraps the Carpet action pack
 * (the official fake-player movement mechanism: the fake player's tick
 * consumes these inputs and Minecraft physics performs the actual movement —
 * jumping, falling, collisions, steps are all native).
 */
public final class FakePlayerMovementController {
    private final FakePlayerHandle handle;
    private boolean active;

    public FakePlayerMovementController(FakePlayerHandle handle) {
        this.handle = handle;
    }

    private ServerPlayerInterface carpetPlayer() {
        return (ServerPlayerInterface) handle.player();
    }

    public void forward(boolean on) {
        this.active = on;
        this.carpetPlayer().getActionPack().setForward(on ? 1.0F : 0.0F);
    }

    public void strafe(float value) {
        this.carpetPlayer().getActionPack().setStrafing(value);
    }

    public void jump(boolean on) {
        if (on) {
            handle.player().jump();
        }
    }

    public void sneak(boolean on) {
        this.carpetPlayer().getActionPack().setSneaking(on);
    }

    public void sprint(boolean on) {
        this.carpetPlayer().getActionPack().setSprinting(on);
    }

    /** Turns toward an X/Z point (smooth, max ~40 degrees per tick). */
    public void lookToward(double targetX, double targetZ) {
        double dx = targetX - handle.x();
        double dz = targetZ - handle.z();
        double desired = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        double current = handle.yaw();
        double delta = MathHelper.wrapDegrees(desired - current);
        double step = Math.max(-40.0, Math.min(40.0, delta));
        float yaw = (float) MathHelper.wrapDegrees(current + step);
        handle.player().setYaw(yaw);
        handle.player().setHeadYaw(yaw);
    }

    /** Releases all inputs. */
    public void stop() {
        this.active = false;
        this.carpetPlayer().getActionPack().stopMovement();
    }

    public boolean isActive() {
        return this.active;
    }
}

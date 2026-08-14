package com.crpi.fakeplayer.control;

import com.crpi.fakeplayer.action.ActionResult;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Smoothly turns the fake player toward a point over two ticks.
 */
public final class LookTask extends ControlTask {
    private static final int DURATION_TICKS = 2;

    private final double targetYaw;
    private final double targetPitch;
    private int elapsed;

    public LookTask(FakePlayerHandle handle, double targetYaw, double targetPitch) {
        super(handle);
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
    }

    /** Calculates yaw/pitch for the player's eyes looking at a point. */
    public static double[] facingToward(FakePlayerHandle handle, Vec3d target) {
        double eyeX = handle.x();
        double eyeY = handle.y() + handle.player().getStandingEyeHeight();
        double eyeZ = handle.z();
        double dx = target.x - eyeX;
        double dy = target.y - eyeY;
        double dz = target.z - eyeZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        double pitch = MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return new double[]{yaw, pitch};
    }

    @Override
    public void tick(long currentTick) {
        this.elapsed++;
        double t = Math.min(1.0, this.elapsed / (double) DURATION_TICKS);
        double startYaw = handle().yaw();
        double startPitch = handle().pitch();
        float yaw = (float) MathHelper.lerpAngleDegrees(t, startYaw, this.targetYaw);
        float pitch = (float) (startPitch + (this.targetPitch - startPitch) * t);
        handle().player().setYaw(yaw);
        handle().player().setPitch(pitch);
        handle().player().setHeadYaw(yaw);
        if (this.elapsed >= DURATION_TICKS) {
            finish(ActionResult.SUCCESS);
        }
    }
}

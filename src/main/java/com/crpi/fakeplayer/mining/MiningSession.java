package com.crpi.fakeplayer.mining;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A single mining attempt driven entirely by the vanilla
 * {@code ServerPlayerInteractionManager}:
 *
 * <ol>
 * <li>{@link #start()} sends START_DESTROY_BLOCK once. The manager records the
 *     mining position and its own {@code update()} (called from the fake
 *     player's normal tick) advances progress using the full vanilla formula
 *     ({@code calcBlockBreakingDelta}: hardness, tool, enchantments, water
 *     penalty) and breaks the block itself when done.</li>
 * <li>{@link #tick()} only polls the world: block gone = success, timeout or
 *     unmineable block = failure.</li>
 * </ol>
 *
 * <p>No mining math is reimplemented here; the session is a lifecycle shell.
 */
public final class MiningSession {
    private static final long TIMEOUT_TICKS = 2400;

    public enum State {
        STARTED,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    private final FakePlayerHandle handle;
    private final BlockPos pos;
    private final Direction direction;
    private State state = State.STARTED;
    private long startTick;
    private long elapsed;
    private float progress;

    MiningSession(FakePlayerHandle handle, BlockPos pos, Direction direction) {
        this.handle = handle;
        this.pos = pos;
        this.direction = direction;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public State state() {
        return this.state;
    }

    public long elapsed() {
        return this.elapsed;
    }

    /** Sends START_DESTROY_BLOCK through the vanilla interaction manager. */
    public void start(long currentTick) {
        this.startTick = currentTick;
        this.handle.player().interactionManager.processBlockBreakingAction(
            this.pos,
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
            this.direction,
            this.handle.world().getHeight(),
            -1
        );
    }

    /**
     * Polls progress. Called every server tick while the action is RUNNING.
     *
     * <p>Progress is accumulated here (Carpet-style) using the vanilla
     * {@code calcBlockBreakingDelta} per tick: the fake player's own tick
     * does not reliably run the interaction manager's internal mining
     * update, so the scheduler drives it instead. The block break itself
     * still goes through the vanilla {@code finishMining}.
     *
     * @return the new session state
     */
    public State tick(long currentTick) {
        if (this.state == State.SUCCESS || this.state == State.FAILED || this.state == State.CANCELLED) {
            return this.state;
        }
        this.state = State.RUNNING;
        this.elapsed = currentTick - this.startTick;
        BlockState block = this.handle.world().getBlockState(this.pos);
        if (block.isAir()) {
            this.state = State.SUCCESS;
            return this.state;
        }
        if (this.elapsed > TIMEOUT_TICKS) {
            this.cancel();
            this.state = State.FAILED;
            return this.state;
        }
        float delta = block.calcBlockBreakingDelta(this.handle.player(), this.handle.world(), this.pos);
        // Unmineable target (bedrock, adventure-mode restrictions, ...): the
        // vanilla delta is 0. Fail fast instead of waiting for the timeout.
        if (delta <= 0) {
            this.cancel();
            this.state = State.FAILED;
            return this.state;
        }
        this.progress += delta;
        if (this.progress >= 1.0F) {
            this.handle.player().interactionManager.finishMining(this.pos, -1, "crpi finished");
            this.state = State.SUCCESS;
        }
        return this.state;
    }

    /** Sends ABORT_DESTROY_BLOCK so the manager releases its mining state. */
    public void cancel() {
        this.handle.player().interactionManager.processBlockBreakingAction(
            this.pos,
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
            this.direction,
            this.handle.world().getHeight(),
            -1
        );
        this.handle.world().setBlockBreakingInfo(this.handle.player().getId(), this.pos, -1);
        this.state = State.CANCELLED;
    }
}

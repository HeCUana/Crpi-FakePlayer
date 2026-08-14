package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.mining.MiningSession;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Mines a block through the vanilla interaction manager. Stateful: the
 * action carries the {@link MiningSession} and runs across ticks.
 */
public final class DigAction extends Action {
    private final BlockPos pos;
    private final Direction direction;
    private MiningSession session;

    public DigAction(FakePlayerHandle handle, long currentTick, BlockPos pos, Direction direction) {
        super(ActionType.DIG, handle, currentTick);
        this.pos = pos;
        this.direction = direction;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public Direction direction() {
        return this.direction;
    }

    public MiningSession session() {
        return this.session;
    }

    public void session(MiningSession session) {
        this.session = session;
    }
}

package com.crpi.fakeplayer.action.impl;

import com.crpi.fakeplayer.action.Action;
import com.crpi.fakeplayer.action.ActionType;
import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Uses (right-clicks) a block through the vanilla interaction manager,
 * covering placement, containers, doors, buttons, levers and mod blocks.
 */
public final class UseAction extends Action {
    private final BlockPos pos;
    private final Direction direction;

    public UseAction(FakePlayerHandle handle, long currentTick, BlockPos pos, Direction direction) {
        super(ActionType.USE, handle, currentTick);
        this.pos = pos;
        this.direction = direction;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public Direction direction() {
        return this.direction;
    }
}

package com.crpi.fakeplayer.navigation.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * World data abstraction for the navigation engine. A* never touches the
 * ServerWorld directly; all block queries go through here so the engine can
 * later run on snapshots (async pathfinding) without changes.
 *
 * <p>Phase 1: synchronous, loaded-chunks-only view.
 */
public final class NavigationWorld {
    private final ServerWorld world;

    public NavigationWorld(ServerWorld world) {
        this.world = world;
    }

    public ServerWorld world() {
        return this.world;
    }

    public boolean isLoaded(BlockPos pos) {
        return this.world.isChunkLoaded(pos);
    }

    public BlockState state(BlockPos pos) {
        return this.world.getBlockState(pos);
    }

    /** Solid for collision purposes (uses the real block movement check). */
    public boolean isSolid(BlockPos pos) {
        return this.state(pos).blocksMovement();
    }

    /** Passable for the player body (feet/head), including fluid check. */
    public boolean isPassable(BlockPos pos) {
        BlockState state = this.state(pos);
        return !state.blocksMovement() && state.getFluidState().isEmpty();
    }

    /** Can a player stand with feet at {@code pos}. */
    public boolean canStandAt(BlockPos pos) {
        return this.isSolid(pos.down())
            && this.isPassable(pos)
            && this.isPassable(pos.up());
    }

    /** Dangerous blocks: lava or fire (Phase 1 minimal set). */
    public boolean isDangerous(BlockPos pos) {
        BlockState state = this.state(pos);
        return state.isOf(Blocks.LAVA) || state.isOf(Blocks.FIRE);
    }
}

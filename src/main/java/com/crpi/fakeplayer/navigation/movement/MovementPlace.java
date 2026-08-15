package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import com.crpi.fakeplayer.navigation.movement.controller.FakePlayerMovementController;
import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Places a block from the inventory to fill the gap in front (a pit too
 * deep to fall into), then walks over it. The block goes through the
 * vanilla {@code setBlockState} with the player's own inventory item —
 * movement afterwards is still native physics.
 */
public final class MovementPlace implements Movement {
    private final PathNode source;
    private final PathNode target;
    private final BlockPos placePos;
    private boolean placed;
    private ItemStack usedStack;

    public MovementPlace(PathNode source, PathNode target, BlockPos placePos) {
        this.source = source;
        this.target = target;
        this.placePos = placePos;
    }

    @Override
    public PathNode target() {
        return this.target;
    }

    @Override
    public double cost() {
        return 5.0;
    }

    @Override
    public void apply(FakePlayerHandle handle, FakePlayerMovementController controller, NavigationWorld world, long tick) {
        if (!this.placed) {
            this.placed = tryPlace(handle, world);
            controller.forward(false);
            return;
        }
        controller.lookToward(this.target.x() + 0.5, this.target.z() + 0.5);
        controller.forward(true);
        controller.jump(false);
    }

    private boolean tryPlace(FakePlayerHandle handle, NavigationWorld world) {
        if (!world.isLoaded(this.placePos)) {
            return false;
        }
        BlockState current = world.state(this.placePos);
        if (!current.isAir()) {
            return true; // someone else filled it; walk on
        }
        var inventory = handle.inventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                BlockState placement = block.getDefaultState();
                if (placement.canPlaceAt(handle.world(), this.placePos)) {
                    handle.world().setBlockState(this.placePos, placement, Block.NOTIFY_ALL);
                    this.usedStack = stack.copy();
                    stack.decrement(1);
                    handle.inventory().markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isComplete(FakePlayerHandle handle, NavigationWorld world) {
        double dx = handle.x() - (this.target.x() + 0.5);
        double dz = handle.z() - (this.target.z() + 0.5);
        return this.placed && dx * dx + dz * dz < 0.4 * 0.4;
    }

    @Override
    public String description() {
        return "Place " + this.placePos.getX() + "," + this.placePos.getY() + "," + this.placePos.getZ();
    }
}

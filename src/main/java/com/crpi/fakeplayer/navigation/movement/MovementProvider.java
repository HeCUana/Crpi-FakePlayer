package com.crpi.fakeplayer.navigation.movement;

import com.crpi.fakeplayer.navigation.path.PathNode;
import com.crpi.fakeplayer.navigation.world.NavigationWorld;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates every legal movement out of a node. Phase 1: 4-direction
 * traverse, 1-block ascend and 1-block descend.
 */
public final class MovementProvider {
    private static final int[][] TRAVERSE = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int MAX_FALL_DISTANCE = 3;

    public List<Movement> generate(NavigationWorld world, PathNode from) {
        List<Movement> movements = new ArrayList<>(16);
        return generate(world, from, movements);
    }

    /** Generation with profile switches (break/place/parkour). */
    public List<Movement> generate(NavigationWorld world, PathNode from, List<Movement> movements) {
        // never query unloaded chunks: the engine only plans through loaded terrain
        net.minecraft.util.math.BlockPos center = new net.minecraft.util.math.BlockPos(from.x(), from.y(), from.z());
        if (!world.isLoaded(center)) {
            return movements;
        }
        for (int[] d : TRAVERSE) {
            addTraverse(world, from, d[0], d[1], movements);
            addAscend(world, from, d[0], d[1], movements);
            addDescend(world, from, d[0], d[1], movements);
            addFall(world, from, d[0], d[1], movements);
            addBreak(world, from, d[0], d[1], movements);
            addPlace(world, from, d[0], d[1], movements);
        }
        for (int[] d : DIAGONALS) {
            addDiagonal(world, from, d[0], d[1], movements);
        }
        for (int[] d : TRAVERSE) {
            addParkour(world, from, d[0], d[1], 1, movements);
            addParkour(world, from, d[0], d[1], 2, movements);
        }
        return movements;
    }

    private void addBreak(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        if (!world.profile().allowBreak) {
            return;
        }
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, from.y(), z);
        net.minecraft.block.BlockState state = world.state(pos);
        // soft blocks only: hard stone needs tools and long mining times (later phase)
        if (!state.blocksMovement() || state.isAir()) {
            return;
        }
        float hardness = state.getHardness(world.world(), pos);
        if (hardness <= 0.0F || hardness > 1.5F) {
            return;
        }
        // the space must be standable once the block is gone
        if (!world.canStandAt(pos)) {
            return;
        }
        out.add(new MovementBreak(from, new PathNode(x, from.y(), z, 0, 0, null, null), pos));
    }

    private void addPlace(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        if (!world.profile().allowPlace) {
            return;
        }
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        net.minecraft.util.math.BlockPos placePos = new net.minecraft.util.math.BlockPos(x, from.y() - 1, z);
        // a pit too deep to fall into: floor absent, nothing under for max fall
        if (world.isSolid(placePos)) {
            return;
        }
        if (!world.state(placePos).isAir()) {
            return;
        }
        // the space above the gap must be clear for the walk-over
        if (!world.isPassable(new net.minecraft.util.math.BlockPos(x, from.y(), z))
            || !world.isPassable(new net.minecraft.util.math.BlockPos(x, from.y() + 1, z))) {
            return;
        }
        out.add(new MovementPlace(from, new PathNode(x, from.y(), z, 0, 0, null, null), placePos));
    }

    private void addParkour(NavigationWorld world, PathNode from, int dx, int dz, int span, List<Movement> out) {
        int x = from.x() + dx * span;
        int z = from.z() + dz * span;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        net.minecraft.util.math.BlockPos target = new net.minecraft.util.math.BlockPos(x, from.y(), z);
        // other side standable at the same height
        if (!world.canStandAt(target)) {
            return;
        }
        // head space at take-off and above the gap
        if (!world.isPassable(new net.minecraft.util.math.BlockPos(from.x(), from.y() + 2, from.z()))) {
            return;
        }
        // the gap itself: every intermediate position must have an empty floor
        // (that is what makes it a gap) but passable body space
        for (int i = 1; i < span; i++) {
            net.minecraft.util.math.BlockPos gapFeet = new net.minecraft.util.math.BlockPos(from.x() + dx * i, from.y(), from.z() + dz * i);
            if (world.isSolid(gapFeet.down())) {
                return; // not a gap
            }
            if (!world.isPassable(gapFeet) || !world.isPassable(gapFeet.up())) {
                return;
            }
        }
        // landing edge: the position right before the gap must be solid floor
        if (!world.isSolid(new net.minecraft.util.math.BlockPos(from.x(), from.y() - 1, from.z()))) {
            return;
        }
        out.add(new MovementParkour(from, new PathNode(x, from.y(), z, 0, 0, null, null), dx, dz, span));
    }

    private boolean targetLoaded(NavigationWorld world, int x, int z) {
        return world.isLoaded(new net.minecraft.util.math.BlockPos(x, 0, z));
    }

    private void addTraverse(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        if (world.canStandAt(new net.minecraft.util.math.BlockPos(x, from.y(), z))) {
            out.add(new MovementTraverse(from, new PathNode(x, from.y(), z, 0, 0, null, null), dx, dz));
        }
    }

    private void addAscend(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        int y = from.y() + 1;
        net.minecraft.util.math.BlockPos feet = new net.minecraft.util.math.BlockPos(x, y, z);
        // target standable (the step block itself is solid at the current
        // level — that is what we stand ON, not a wall) and jump space free
        if (world.canStandAt(feet)
            && world.isPassable(new net.minecraft.util.math.BlockPos(from.x(), from.y() + 2, from.z()))) {
            out.add(new MovementAscend(from, new PathNode(x, y, z, 0, 0, null, null), dx, dz));
        }
    }

    private void addDescend(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        int y = from.y() - 1;
        net.minecraft.util.math.BlockPos feet = new net.minecraft.util.math.BlockPos(x, y, z);
        // 1-block drop: target standable, current foot space passable so the
        // player can step down into it
        if (world.canStandAt(feet)
            && world.isPassable(new net.minecraft.util.math.BlockPos(x, from.y(), z))
            && world.isPassable(new net.minecraft.util.math.BlockPos(x, from.y() + 1, z))) {
            out.add(new MovementDescend(from, new PathNode(x, y, z, 0, 0, null, null), dx, dz));
        }
    }

    private void addFall(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        for (int drop = 2; drop <= MAX_FALL_DISTANCE; drop++) {
            int y = from.y() - drop;
            net.minecraft.util.math.BlockPos feet = new net.minecraft.util.math.BlockPos(x, y, z);
            // target standable and every level of the fall shaft passable
            // (head space all the way down)
            boolean clear = true;
            for (int i = 0; i <= drop; i++) {
                if (!world.isPassable(new net.minecraft.util.math.BlockPos(x, from.y() - i, z))) {
                    clear = false;
                    break;
                }
            }
            if (clear && world.canStandAt(feet)) {
                out.add(new MovementFall(from, new PathNode(x, y, z, 0, 0, null, null), dx, dz, drop));
            }
        }
    }

    private void addDiagonal(NavigationWorld world, PathNode from, int dx, int dz, List<Movement> out) {
        int x = from.x() + dx;
        int z = from.z() + dz;
        if (!targetLoaded(world, x, z)) {
            return;
        }
        net.minecraft.util.math.BlockPos feet = new net.minecraft.util.math.BlockPos(x, from.y(), z);
        // no corner-cutting: both adjacent orthogonal positions must be passable
        boolean cornerClear = world.isPassable(new net.minecraft.util.math.BlockPos(from.x() + dx, from.y(), from.z()))
            && world.isPassable(new net.minecraft.util.math.BlockPos(from.x(), from.y(), from.z() + dz))
            && world.isPassable(new net.minecraft.util.math.BlockPos(from.x() + dx, from.y() + 1, from.z()))
            && world.isPassable(new net.minecraft.util.math.BlockPos(from.x(), from.y() + 1, from.z() + dz));
        if (cornerClear && world.canStandAt(feet)) {
            out.add(new MovementDiagonal(from, new PathNode(x, from.y(), z, 0, 0, null, null), dx, dz));
        }
    }
}

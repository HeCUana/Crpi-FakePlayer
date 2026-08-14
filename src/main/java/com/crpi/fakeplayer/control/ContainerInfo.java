package com.crpi.fakeplayer.control;

import java.util.List;
import net.minecraft.util.math.BlockPos;

/**
 * Read-only container summary used by the environment APIs.
 */
public record ContainerInfo(BlockPos pos, String blockId, boolean canOpen, List<ItemStackSnapshot> items) {
}

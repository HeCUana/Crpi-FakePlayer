package com.crpi.fakeplayer.container;

import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Result of scanning one container in the world. Read-only: the scanner
 * never opens a GUI and never modifies the container.
 *
 * @param pos      container position
 * @param blockId  block identifier (e.g. {@code minecraft:chest})
 * @param canOpen  whether the fake player could open this container right now
 * @param items    merged item entries (same identifier summed, empty slots omitted)
 */
public record ContainerScanResult(
    BlockPos pos,
    Identifier blockId,
    boolean canOpen,
    List<ItemStackInfo> items
) {
}

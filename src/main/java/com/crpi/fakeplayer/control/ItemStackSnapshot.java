package com.crpi.fakeplayer.control;

import net.minecraft.util.Identifier;

/**
 * Immutable snapshot of an item: identifier + count. Cannot mutate the
 * fake player's inventory.
 */
public record ItemStackSnapshot(Identifier itemId, int count) {
}

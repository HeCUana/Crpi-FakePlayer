package com.crpi.fakeplayer.container;

import net.minecraft.util.Identifier;

/**
 * A merged item entry: one identifier with a summed count across all slots.
 */
public record ItemStackInfo(Identifier itemId, int count) {
}

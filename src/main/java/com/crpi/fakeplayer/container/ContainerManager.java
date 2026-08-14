package com.crpi.fakeplayer.container;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

/**
 * Read-only helpers around a fake player's current container.
 */
public final class ContainerManager {
    private ContainerManager() {
    }

    /** Lists the container contents as "index: item x count" lines. */
    public static List<String> describeContents(FakePlayerHandle handle) {
        ContainerContext context = ContainerContext.of(handle);
        if (context == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        ScreenHandler handler = context.handler();
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) {
                lines.add("#" + i + " " + stack.getItem().getName().getString() + " x" + stack.getCount());
            }
        }
        return lines;
    }
}

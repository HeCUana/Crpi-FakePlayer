package com.crpi.fakeplayer.container;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Scans the loaded chunks around a fake player for containers and returns a
 * read-only inventory summary for each.
 *
 * <p>Read-only by design: no GUI is opened, no block entity is modified, no
 * chunk is loaded. Containers are detected through the vanilla
 * {@link Inventory} interface, so standard mod containers are picked up
 * automatically.
 */
public final class ContainerScanner {
    private ContainerScanner() {
    }

    /**
     * Scans a cube of {@code radius} blocks around the fake player.
     *
     * @param handle the fake player
     * @param radius scan radius in blocks (already clamped by the caller)
     * @return one result per container found (double chests reported once)
     */
    public static List<ContainerScanResult> scan(FakePlayerHandle handle, int radius) {
        ServerWorld world = handle.world();
        BlockPos center = handle.player().getBlockPos();
        List<ContainerScanResult> results = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            // never load chunks for scanning; only inspect loaded ones
            if (!world.isChunkLoaded(pos)) {
                continue;
            }
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof Inventory inventory)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            // double chests: report the primary half only, its inventory already
            // covers both halves
            if (state.getBlock() instanceof ChestBlock
                && ChestBlock.getDoubleBlockType(state) == DoubleBlockProperties.Type.SECOND) {
                continue;
            }
            boolean canOpen = canOpen(handle, blockEntity, pos);
            List<ItemStackInfo> items = collectItems(inventory);
            results.add(new ContainerScanResult(pos.toImmutable(), Registries.BLOCK.getId(state.getBlock()), canOpen, items));
        }
        return results;
    }

    /**
     * Best-effort vanilla "could this player open this container right now":
     * block entity alive, within the player's interaction range, and unlocked
     * (for lockable containers). Line-of-sight occlusion is NOT simulated;
     * see the feature docs for the exact limitation.
     */
    private static boolean canOpen(FakePlayerHandle handle, BlockEntity blockEntity, BlockPos pos) {
        if (blockEntity.isRemoved()) {
            return false;
        }
        if (!handle.player().canInteractWithBlockAt(pos, 1.0)) {
            return false;
        }
        if (blockEntity instanceof LootableContainerBlockEntity lockable) {
            return lockable.checkUnlocked(handle.player());
        }
        return true;
    }

    /** Merges the container contents by item identifier, skipping empty slots. */
    private static List<ItemStackInfo> collectItems(Inventory inventory) {
        Map<Identifier, Integer> merged = new LinkedHashMap<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            Identifier id = Registries.ITEM.getId(stack.getItem());
            merged.merge(id, stack.getCount(), Integer::sum);
        }
        List<ItemStackInfo> items = new ArrayList<>(merged.size());
        for (Map.Entry<Identifier, Integer> entry : merged.entrySet()) {
            items.add(new ItemStackInfo(entry.getKey(), entry.getValue()));
        }
        return items;
    }
}

package io.github.zowja.structure;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class LootChest {

    private int totalweight;
    public short numOfLoot;
    private final List<LootItem> loot = new ArrayList<>();

    public void addLoot(final short itemId, final short meta, final short maxStackSize, final short weight) {
        final LootItem lootItem = new LootItem(itemId, meta, maxStackSize, weight);
        this.totalweight += weight;
        this.loot.add(lootItem);
    }

    public Collection<ItemStack> getLoot(final Random rand) {
        final Collection<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < this.numOfLoot; i++) {
            final LootItem lootItem = this.getSingleLootItem(rand);
            items.add(lootItem.toItemStack(rand));
        }
        return items;
    }

    public boolean hasLoot() {
        return this.loot.isEmpty();
    }

    private LootItem getSingleLootItem(final Random rand) {
        final int roll = rand.nextInt(this.totalweight) + 1;
        int total = 0;
        LootItem lootItem = null;
        for (final LootItem item : this.loot) {
            lootItem = item;
            if (total + item.weight > roll)
                break;
            total += item.weight;
        }
        return lootItem;
    }

    private class LootItem {
        short itemID, meta, maxStackSize, weight;

        LootItem(final short itemID, final short meta, final short maxStackSize, final short weight) {
            this.itemID = itemID;
            this.meta = meta;
            this.maxStackSize = maxStackSize;
            this.weight = weight;
        }

        ItemStack toItemStack(final Random rand) {
            final int amount = rand.nextInt(this.maxStackSize) + 1;
            return new ItemStack(itemID,amount,meta);
        }
    }

}

package plugin.structure;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class LootChest {

    private int totalweight;
    public short numOfLoot;
    public List<LootItem> loot = new ArrayList<>();

    public void addLoot(final short item, final short meta, final short num, final short weight) {
        final LootItem loot = new LootItem();
        loot.itemID = item;
        loot.metadata = meta;
        loot.numberInStack = num;
        loot.weight = weight;
        this.totalweight += weight;
        this.loot.add(loot);
    }

    public Collection<ItemStack> getLoot(final Random rand) {
        final Collection<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < this.numOfLoot; i++) {
            final LootItem item = this.getSingleLootItem(rand);
            items.add(new ItemStack(item.itemID, rand.nextInt(item.numberInStack) + 1, item.metadata));
        }
        return items;
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
        short itemID, metadata, numberInStack, weight;
    }

}

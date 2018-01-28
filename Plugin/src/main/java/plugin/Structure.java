package plugin;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class Structure {

	public int random;
	public boolean hasBiome = true, hasInitial = true, hasDeep = true, hasMeta = true;

	public short worldType;
	public double topTemp, lowTemp, topHumidity, lowHumidity;
	public double commonality;
	public short yTop, yBottom;
	public short[][] initialCheck;
	public short[][][] deepCheck;
	public short[][][] structure;
	public short[][] metadata;
	public final List<short[]> multiChecks = new ArrayList<>();
	public final List<LootChest> chests = new ArrayList<>();
	public final List<RandomNumberSet> randoms = new ArrayList<>();
	public final List<Spawner> spawners = new ArrayList<>();

	public LootChest getNewChest() {
		this.chests.add(new LootChest());
		return this.chests.get(this.chests.size() - 1);
	}

	public Spawner getNewSpawner() {
		this.spawners.add(new Spawner());
		return this.spawners.get(this.spawners.size() - 1);
	}

	public void createNewRandom(int[] number, int[] weight) {
		this.randoms.add(new RandomNumberSet(weight, number));
	}

	class LootChest {
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
	}

	class LootItem {
		short itemID, metadata, numberInStack, weight;
	}

	class Spawner {
		final List<String> mobIDs = new ArrayList<>();
		final List<Short> weights = new ArrayList<>();
		int totalweight;
	}

	class RandomNumberSet {
		final int[] weight, number;
		final int totalRandomWeight;

		RandomNumberSet(final int[] weight, final int[] number){
			this.weight = weight;
			this.number = number;
			this.totalRandomWeight = Arrays.stream(weight).sum();
		}
	}

}

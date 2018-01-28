package plugin.structure;

import java.util.ArrayList;
import java.util.List;

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

}

package plugin.structure;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;

public class Structure {

	public int random;
	public boolean hasBiome, hasInitial, hasDeep, hasMeta;

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

	public int getWidth() {
	    return this.structure.length;
    }

    public int getHeight() {
	    return this.structure[0].length;
    }

    public int getLength() {
	    return this.structure[0][0].length;
    }

    public int getId(final int x, final int y, final int z) {
	    return this.structure[x][y][z];
    }

	public void addChest(final LootChest chest) {
		this.chests.add(chest);
	}

	public void addSpawner(final Spawner spawner) {
		this.spawners.add(spawner);
	}

	public void addRandom(final RandomNumberSet random) {
		this.randoms.add(random);
	}

	public boolean canGenerateAt(final Location loc, final long seed) {
        if (!this.checkWorldType(loc.getWorld())) return false;
        if (!this.checkBiome(loc, seed)) return false;
	    if (!this.initCheck(loc)) return false;
	    if (!this.deepCheck(loc)) return false;
	    return true;
    }

    private boolean checkWorldType(final World world) {
        final int worldTypeId = world.getEnvironment().getId();
        if(this.worldType == 1 && worldTypeId !=  0) return false;
        if(this.worldType == 2 && worldTypeId != -1) return false;
        if(this.worldType == 3 && worldTypeId !=  1) return false;
        return true;
    }

    private boolean checkBiome(final Location loc, final long seed) {
	    if (!this.hasBiome) return true;
	    double temperature = loc.getBlock().getTemperature();
        if (temperature > this.topTemp) return false;
        if (temperature < this.lowTemp) return false;
        double humidity = Utils.getHumidity(seed,loc.getBlockX(), loc.getBlockZ());
        if (humidity > this.topHumidity) return false;
        if (humidity < this.lowHumidity) return false;
        return true;
    }

    private boolean initCheck(final Location loc) {
	    if (!this.hasInitial) return true;
        for (short[] check : this.initialCheck) {
            final Block block = loc.clone().add(check[0], check[1], check[2]).getBlock();
            if (check[3] > -1) {
                if (block.getTypeId() != check[3])
                    return false;
                if (block.getData() != (byte)check[4])
                    return false;
            }
            if (check[3] < -31) {
                if (!this.multiCheck(block.getLocation(), check[3]))
                    return false;
            }
        }
	    return true;
    }

    private boolean deepCheck(final Location loc) {
	    if (!this.hasDeep) return true;
        for (int x = 0; x < this.getWidth(); x++) {
            for (int y = 0; y < this.getHeight(); y++) {
                for (int z = 0; z < this.getLength(); z++) {
                    final Block block = loc.clone().add(x, y, z).getBlock();
                    if (this.deepCheck[x][y][z] > -1) {
                        if (block.getTypeId() != this.deepCheck[x][y][z])
                            return false;
                    }
                    if (this.deepCheck[x][y][z] < -31) {
                        if (!this.multiCheck(block.getLocation(),this.deepCheck[x][y][z]))
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean multiCheck(final Location loc, final int checkNum) {
        final int blockId = loc.getBlock().getTypeId();
        final short[] checks = this.multiChecks.get(-checkNum - 32);
        for (final int check : checks) {
            if (blockId != check)
                return false;
        }
        return true;
    }

}

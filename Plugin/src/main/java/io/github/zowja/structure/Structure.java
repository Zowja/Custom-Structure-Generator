package io.github.zowja.structure;

import io.github.zowja.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Structure {

    public int seed;

    public short worldType;
    private double[] biome;
    public double commonality;
    private short[] heightLimit;
    public short[][] initialCheck;
    public short[][][] deepCheck;
    public short[][][] structure;
    public short[][] metadata;
    public final List<short[]> multiChecks = new ArrayList<>();
    public final List<LootChest> chests = new ArrayList<>();
    public final List<RandomNumberSet> randoms = new ArrayList<>();
    public final List<Spawner> spawners = new ArrayList<>();

    public void setBiome(final double minTemperature, final double maxTemperature, final double minHumidity, final double maxHumidity) {
        this.biome = new double[]{
                minTemperature > maxTemperature ? maxTemperature : minTemperature,
                minTemperature > maxTemperature ? minTemperature : maxTemperature,
                minHumidity > maxHumidity ? maxHumidity : minHumidity,
                minHumidity > maxHumidity ? minHumidity : maxHumidity
        };
    }

    public boolean hasBiome() {
        return this.biome != null;
    }

    public double getMinTemperature() {
        return this.biome[0];
    }

    public double getMaxTemperature() {
        return this.biome[1];
    }

    public double getMinHumidity() {
        return this.biome[2];
    }

    public double getMaxHumidity() {
        return this.biome[3];
    }

    public void setHeightLimit(short min, short max) {
        if (min < 0) min = 0;
        if (min > 128) min = 128;
        if (max < 0) max = 0;
        if (max > 128) max = 128;
        this.heightLimit = min > max ? new short[]{max, min} : new short[]{min, max};
    }

    public int getLimitedRandomY(final Random rand) {
        return this.getHeightLimitMin() + rand.nextInt(this.getHeightLimitMax() - this.getHeightLimitMin() + 1);
    }

    public short getHeightLimitMin() {
        return this.heightLimit != null ? this.heightLimit[0] : 0;
    }

    public short getHeightLimitMax() {
        return this.heightLimit != null ? this.heightLimit[1] : 128;
    }

    public boolean hasInitial() {
        return this.initialCheck != null;
    }

    public boolean hasDeep() {
        return this.deepCheck != null;
    }

    public boolean hasMeta() {
        return this.metadata != null;
    }

    public int getWidth() {
        return this.structure.length;
    }

    public int getHeight() {
        return this.structure[0].length;
    }

    public int getLength() {
        return this.structure[0][0].length;
    }

    public int getBlockId(final int x, final int y, final int z) {
        return this.structure[x][y][z];
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
        if (!this.hasBiome()) return true;
        double temperature = loc.getBlock().getTemperature();
        if (temperature > this.biome[1]) return false;
        if (temperature < this.biome[0]) return false;
        double humidity = Utils.getHumidity(seed,loc.getBlockX(), loc.getBlockZ());
        if (humidity > this.biome[3]) return false;
        if (humidity < this.biome[2]) return false;
        return true;
    }

    private boolean initCheck(final Location loc) {
        if (!this.hasInitial()) return true;
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
        if (!this.hasDeep()) return true;
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

    private boolean multiCheck(final Location loc, final int checkId) {
        final int blockId = loc.getBlock().getTypeId();
        final short[] ids = this.multiChecks.get(-checkId - 32);
        for (final int id : ids) {
            if (blockId != id)
                return false;
        }
        return true;
    }

}

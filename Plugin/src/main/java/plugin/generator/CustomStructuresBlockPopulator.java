package plugin.generator;

import net.minecraft.server.NoiseGeneratorOctaves2;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plugin.CustomStructuresPlugin;
import plugin.structure.LootChest;
import plugin.structure.RandomNumberSet;
import plugin.structure.Spawner;
import plugin.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class CustomStructuresBlockPopulator extends BlockPopulator {

    @Override
    public void populate(final World world, final Random rand, final Chunk chunk) {
        final long baseSeed = world.getSeed() * chunk.getX() + chunk.getZ();
        rand.setSeed(baseSeed);
        int x = rand.nextInt(16);
        int z = rand.nextInt(16);
        int y;

        double temp = world.getTemperature(x + (chunk.getX()*16), z + (chunk.getZ()*16)) * 100;
        double humidity = this.getHumidity(world.getSeed(), x + chunk.getX()*16, z + chunk.getZ()*16);

        for (Structure structure : CustomStructuresPlugin.structures) {
            if(structure.worldType == 1 && world.getEnvironment().getId() != 0){
                continue;
            }
            if(structure.worldType == 2 && world.getEnvironment().getId() != -1){
                continue;
            }
            if(structure.worldType == 3 && world.getEnvironment().getId() != 1){
                continue;
            }
            if (structure.hasBiome && (temp > structure.topTemp || temp < structure.lowTemp
                    || humidity > structure.topHumidity || humidity < structure.lowHumidity)) {
                continue;
            }
            double spawnAttempts = structure.commonality;
            //Reset seed so that the individual structures are consistent across all seeds.
            rand.setSeed(baseSeed + structure.random);
            attempt: while (spawnAttempts > 0) {
                double random = rand.nextDouble();
                if (random > spawnAttempts) {
                    break attempt;
                }
                y = structure.yBottom + rand.nextInt(structure.yTop - structure.yBottom + 1);
                x = rand.nextInt(16) + chunk.getX()*16;
                z = rand.nextInt(16) + chunk.getZ()*16;
                spawnAttempts--;
                if (structure.hasInitial) {
                    for (short[] check : structure.initialCheck)
                        if (check[3] > -1) {
                            if (world.getBlockAt(x + check[0], y + check[1], z + check[2]).getTypeId() != check[3])
                                continue attempt;
                            else if(check[4] >= 0 && (world.getBlockAt(x + check[0], y + check[1], z + check[2]).getData() == (byte)check[4])){
                                continue attempt;
                            }
                        } else if (check[3] < -31) {
                            short[] checks = structure.multiChecks.get(-check[3] - 32);
                            boolean checked = false;
                            int blockID = world.getBlockAt(x + check[0], y + check[1], z + check[2]).getTypeId();
                            for (int innerCheck : checks) {
                                if (innerCheck == blockID) {
                                    checked = true;
                                    break;
                                }
                            }
                            if (!checked)
                                continue attempt;
                        }
                }
                if (structure.hasDeep) {
                    for (int xx = 0; xx < structure.deepCheck.length; xx++) {
                        for (int yy = 0; yy < structure.deepCheck[xx].length; yy++) {
                            for (int zz = 0; zz < structure.deepCheck[xx][yy].length; zz++) {
                                if (structure.deepCheck[xx][yy][zz] > -1) {
                                    if (world.getBlockAt(x + xx, y + yy, z + zz).getTypeId() != structure.deepCheck[xx][yy][zz]) {
                                        continue attempt;
                                    }
                                } else if (structure.deepCheck[xx][yy][zz] < -31) {
                                    short[] checks = structure.multiChecks.get(-structure.deepCheck[xx][yy][zz] - 32);
                                    boolean checked = false;
                                    int blockID = world.getBlockAt(x + xx, y + yy, z + zz).getTypeId();
                                    for (int innerCheck : checks) {
                                        if (innerCheck == blockID) {
                                            checked = true;
                                            break;
                                        }
                                    }
                                    if (!checked)
                                        continue attempt;
                                }
                            }
                        }
                    }
                }
                for (int xx = 0; xx < structure.structure.length; xx++) {
                    for (int yy = 0; yy < structure.structure[xx].length; yy++) {
                        for (int zz = 0; zz < structure.structure[xx][yy].length; zz++) {
                            if (structure.structure[xx][yy][zz] > -1)
                                world.getBlockAt(x + xx, y + yy, z + zz).setTypeId(structure.structure[xx][yy][zz]);

                            else if (structure.structure[xx][yy][zz] < -31) {
                                if (structure.structure[xx][yy][zz] > -structure.randoms.size() - 32) {
                                    RandomNumberSet randomNumSet = structure.randoms
                                            .get(-structure.structure[xx][yy][zz] - 32);
                                    int roll = rand.nextInt(randomNumSet.totalRandomWeight);
                                    int total = 0, result = 0;
                                    for (int loop = 0; loop < randomNumSet.number.length; loop++) {
                                        result = randomNumSet.number[loop];
                                        if (total + randomNumSet.weight[loop] > roll) {
                                            break;
                                        }
                                        total += randomNumSet.weight[loop];
                                    }
                                    if (result > -1) {
                                        world.getBlockAt(x + xx, y + yy, z + zz).setTypeId(result);
                                    } else if (result > -structure.chests.size() - structure.randoms.size() - 32) {
                                        generateChest(x + xx, y + yy, z + zz, result, rand, structure, world);
                                    } else if (result > -structure.spawners.size() - structure.chests.size()
                                            - structure.randoms.size() - 32) {
                                        generateSpawner(x + xx, y + yy, z + zz, result, rand, structure, world);
                                    }
                                } else if (structure.structure[xx][yy][zz] > -structure.chests.size()
                                        - structure.randoms.size() - 32) {
                                    generateChest(x + xx, y + yy, z + zz, structure.structure[xx][yy][zz], rand,
                                            structure, world);
                                } else if (structure.structure[xx][yy][zz] > -structure.spawners.size()
                                        - structure.chests.size() - structure.randoms.size() - 32) {
                                    generateSpawner(x + xx, y + yy, z + zz, structure.structure[xx][yy][zz], rand,
                                            structure, world);
                                }
                            }
                        }
                    }
                }
                if (structure.hasMeta) {
                    for (short[] meta : structure.metadata) {
                        world.getBlockAt(meta[0] + x, meta[1] + y, meta[2] + z).setData((byte) meta[3]);;
                    }
                }
            }
        }
    }

    private double getHumidity(final long seed, final int x, final int z) {
        final NoiseGeneratorOctaves2 humOctave = new NoiseGeneratorOctaves2(new Random(seed * 39811L), 4);
        final NoiseGeneratorOctaves2 extraOctave = new NoiseGeneratorOctaves2(new Random(seed * 543321L), 2);
        double[] humid = null;
        double[] extra = null;
        humid = humOctave.a(humid, (double) x, (double) z, 1, 1, 0.05000000074505806D, 0.05000000074505806D, 0.3333333333333333D);
        extra = extraOctave.a(extra, (double) x, (double) z, 1, 1, 0.25D, 0.25D, 0.5882352941176471D);
        double humidity = ((humid[0] * 0.15D + 0.5D) * 0.998 + (extra[0] * 1.1D + 0.5D) * 0.002)*100;
        if(humidity < 0.0D) humidity = 0.0D;
        if(humidity > 100.0D) humidity = 100.0D;
        return humidity;
    }

    private void generateChest(int x, int y, int z, int id, Random rand, Structure structure, World world) {
        world.getBlockAt(x, y, z).setType(Material.CHEST);
        final Inventory inventory = ((ContainerBlock)world.getBlockAt(x, y, z).getState()).getInventory();
        final List<Integer> emptySlots = new ArrayList<>();
        IntStream.range(0, inventory.getSize()).forEach(emptySlots::add);
        final LootChest chest = structure.chests.get(-id - structure.randoms.size() - 32);
        for (final ItemStack item : chest.getLoot(rand)) {
            final int slot = rand.nextInt(emptySlots.size());
            inventory.setItem(slot, item);
            emptySlots.remove(slot);
        }
    }

    private void generateSpawner(int x, int y, int z, int id, Random rand, Structure structure, World world) {
        world.getBlockAt(x, y, z).setTypeId(52);
        Spawner spawner = structure.spawners
                .get(-id - structure.chests.size() - structure.randoms.size() - 32);
        int roll = rand.nextInt(spawner.totalweight);
        int total = 0;
        String mobID = null;
        for (int loop = 0; loop < spawner.mobIDs.size(); loop++) {
            mobID = spawner.mobIDs.get(loop);
            if (total + spawner.weights.get(loop) > roll) {
                break;
            }
            total += spawner.weights.get(loop);
        }
        ((CreatureSpawner)world.getBlockAt(x, y, z).getState()).setCreatureTypeId(mobID);
    }

}

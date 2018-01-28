package plugin.generator;

import net.minecraft.server.NoiseGeneratorOctaves2;
import org.bukkit.Chunk;
import org.bukkit.Location;
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

        for (final Structure structure : CustomStructuresPlugin.structures) {
            //Reset seed so that the individual structures are consistent across all seeds.
            rand.setSeed(baseSeed + structure.random);

            double spawnAttempts = structure.commonality;
            while (spawnAttempts > 0) {
                double random = rand.nextDouble();
                if (random > spawnAttempts)
                    break;

                int y = structure.yBottom + rand.nextInt(structure.yTop - structure.yBottom + 1);
                int x = rand.nextInt(16) + chunk.getX() * 16;
                int z = rand.nextInt(16) + chunk.getZ() * 16;
                spawnAttempts--;

                final Location loc = new Location(world, x, y, z);
                if (!structure.canGenerateAt(loc, baseSeed)) continue;

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

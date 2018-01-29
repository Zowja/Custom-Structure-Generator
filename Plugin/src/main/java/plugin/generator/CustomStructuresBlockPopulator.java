package plugin.generator;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

        for (final Structure struct : CustomStructuresPlugin.structures) {
            //Reset seed so that the individual structures are consistent across all seeds.
            rand.setSeed(baseSeed + struct.random);

            double spawnAttempts = struct.commonality;
            while (spawnAttempts > 0) {
                final double random = rand.nextDouble();
                if (random > spawnAttempts)
                    break;

                spawnAttempts--;

                final int originX = rand.nextInt(16) + chunk.getX() * 16;
                final int originY = struct.yBottom + rand.nextInt(struct.yTop - struct.yBottom + 1);
                final int originZ = rand.nextInt(16) + chunk.getZ() * 16;

                final Location loc = new Location(world, originX, originY, originZ);
                if (!struct.canGenerateAt(loc, baseSeed)) continue;

                final Block origin = loc.getBlock();
                for (int x = 0; x < struct.getWidth(); x++) {
                    for (int y = 0; y < struct.getHeight(); y++) {
                        for (int z = 0; z < struct.getLength(); z++) {
                            final Block block = origin.getRelative(x, y, z);
                            final int id = struct.getId(x,y,z);
                            this.setBlock(block, struct, id, rand);
                        }
                    }
                }
                this.setMeta(origin, struct);
            }
        }
    }

    private void setBlock(final Block block, final Structure struct, final int id, final Random rand) {
        if (id > -1) block.setTypeId(id);
        if (id < -31) {
            if (id > -struct.randoms.size() - 32) {
                final RandomNumberSet randomNumSet = struct.randoms.get(-id - 32);
                final int randomId = randomNumSet.getNumber(rand);
                this.setBlock(block, struct, randomId, rand); // TODO: secure against infinite loop (when random contains itself)
            } else if (id > -struct.chests.size() - struct.randoms.size() - 32) {
                this.generateChest(block, id, rand, struct);
            } else if (id > -struct.spawners.size() - struct.chests.size() - struct.randoms.size() - 32) {
                this.generateSpawner(block, id, rand, struct);
            }
        }
    }

    private void setMeta(final Block origin, final Structure struct) {
        if (!struct.hasMeta) return;
        for (final short[] meta : struct.metadata)
            origin.getRelative(meta[0], meta[1], meta[2]).setData((byte) meta[3]);
    }

    private void generateChest(final Block block, final int id, final Random rand, final Structure structure) {
        block.setType(Material.CHEST);
        final Inventory inventory = ((ContainerBlock)block.getState()).getInventory();
        final List<Integer> emptySlots = new ArrayList<>();
        IntStream.range(0, inventory.getSize()).forEach(emptySlots::add);
        final LootChest chest = structure.chests.get(-id - structure.randoms.size() - 32);
        for (final ItemStack item : chest.getLoot(rand)) {
            final int slot = rand.nextInt(emptySlots.size());
            inventory.setItem(slot, item);
            emptySlots.remove(slot);
        }
    }

    private void generateSpawner(final Block block, final int id, final Random rand, final Structure structure) {
        block.setType(Material.MOB_SPAWNER);
        final Spawner spawner = structure.spawners.get(-id - structure.chests.size() - structure.randoms.size() - 32);
        final String mobID = spawner.getMobId(rand);
        ((CreatureSpawner)block.getState()).setCreatureTypeId(mobID);
    }

}

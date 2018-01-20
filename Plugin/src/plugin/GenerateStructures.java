package plugin;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.block.ContainerBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.NoiseGeneratorOctaves2;

public class GenerateStructures extends WorldListener {
	
	public void onChunkPopulate(ChunkPopulateEvent event){
		Random rand = new Random(event.getWorld().getSeed()*event.getChunk().getX()+event.getChunk().getZ());
		NoiseGeneratorOctaves2 humOctave = new NoiseGeneratorOctaves2(new Random(event.getWorld().getSeed() * 9871L), 4);
		NoiseGeneratorOctaves2 extraOctave = new NoiseGeneratorOctaves2(new Random(event.getWorld().getSeed() * 543321L), 2);
		int x = rand.nextInt(16);
		int z = rand.nextInt(16);
		int y;
		double[] humid = null;
		double[] extra = null;
		humid = humOctave.a(humid, (double) x + (event.getChunk().getX()*16), (double) z + (event.getChunk().getZ()*16), 1, 1, 0.05000000074505806D, 0.05000000074505806D, 0.3333333333333333D);
		extra = extraOctave.a(extra, (double) x + (event.getChunk().getX()*16), (double) z + (event.getChunk().getZ()*16), 1, 1, 0.25D, 0.25D, 0.5882352941176471D);
		
		double temp = event.getWorld().getTemperature(x + (event.getChunk().getX()*16), z + (event.getChunk().getZ()*16)) * 100;
		double humidity = ((humid[0] * 0.15D + 0.5D) * 0.998 + (extra[0] * 1.1D + 0.5D) * 0.002)*100;
		if(humidity < 0.0D)
        {
           humidity = 0.0D;
        }
		if(humidity > 100.0D)
        {
			humidity = 100.0D;
        }
		for (Structure structure : Plugin.structures) {
			if(structure.type == 1 && event.getWorld().getEnvironment().getId() != 0){
				continue;
			}
			if(structure.type == 2 && event.getWorld().getEnvironment().getId() != -1){
				continue;
			}
			if(structure.type == 3 && event.getWorld().getEnvironment().getId() != 1){
				continue;
			}
			if (structure.hasBiome && (temp > structure.topTemp || temp < structure.lowTemp
					|| humidity > structure.topHumidity || humidity < structure.lowHumidity)) {
				continue;
			}
			double spawnAttempts = structure.commonality;
			//Reset seed so that the individual structures are consistent across all seeds.
			rand = new Random(event.getWorld().getSeed()*event.getChunk().getX()+event.getChunk().getZ());
			attempt: while (spawnAttempts > 0) {
				double random = rand.nextDouble();
				if (random > spawnAttempts) {
					break attempt;
				}
				y = structure.yBottom + rand.nextInt(structure.yTop - structure.yBottom + 1);
				x = rand.nextInt(16) + event.getChunk().getX()*16;
				z = rand.nextInt(16) + event.getChunk().getZ()*16;
				spawnAttempts--;
				if (structure.hasInitial) {
					for (short[] check : structure.initialCheck)
						if (check[3] > -1) {
							if (event.getWorld().getBlockAt(x + check[0], y + check[1], z + check[2]).getTypeId() != check[3])
								continue attempt;
							else if(check[4] >= 0 && (event.getWorld().getBlockAt(x + check[0], y + check[1], z + check[2]).getData() == (byte)check[4])){
								continue attempt;
							}
						} else if (check[3] < -31) {
							short[] checks = structure.multiChecks.get(-check[3] - 32);
							boolean checked = false;
							int blockID = event.getWorld().getBlockAt(x + check[0], y + check[1], z + check[2]).getTypeId();
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
									if (event.getWorld().getBlockAt(x + xx, y + yy, z + zz).getTypeId() != structure.deepCheck[xx][yy][zz]) {
										continue attempt;
									}
								} else if (structure.deepCheck[xx][yy][zz] < -31) {
									short[] checks = structure.multiChecks.get(-structure.deepCheck[xx][yy][zz] - 32);
									boolean checked = false;
									int blockID = event.getWorld().getBlockAt(x + xx, y + yy, z + zz).getTypeId();
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
								event.getWorld().getBlockAt(x + xx, y + yy, z + zz).setTypeId(structure.structure[xx][yy][zz]);

							else if (structure.structure[xx][yy][zz] < -31) {
								if (structure.structure[xx][yy][zz] > -structure.randoms.size() - 32) {
									Structure.randomNumberSet randomNumSet = structure.randoms
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
										event.getWorld().getBlockAt(x + xx, y + yy, z + zz).setTypeId(result);
									} else if (result > -structure.chests.size() - structure.randoms.size() - 32) {
										generateChest(x + xx, y + yy, z + zz, result, rand, structure, event);
									} else if (result > -structure.spawners.size() - structure.chests.size()
											- structure.randoms.size() - 32) {
										generateSpawner(x + xx, y + yy, z + zz, result, rand, structure, event);
									}
								} else if (structure.structure[xx][yy][zz] > -structure.chests.size()
										- structure.randoms.size() - 32) {
									generateChest(x + xx, y + yy, z + zz, structure.structure[xx][yy][zz], rand,
											structure, event);
								} else if (structure.structure[xx][yy][zz] > -structure.spawners.size()
										- structure.chests.size() - structure.randoms.size() - 32) {
									generateSpawner(x + xx, y + yy, z + zz, structure.structure[xx][yy][zz], rand,
											structure, event);
								}
							}
						}
					}
				}
				if (structure.hasMeta) {
					for (short[] meta : structure.metadata) {
						event.getWorld().getBlockAt(meta[0] + x, meta[1] + y, meta[2] + z).setData((byte) meta[3]);;
					}
				}
			}
		}
	}
	
	public static void generateChest(int x, int y, int z, int id, Random rand, Structure structure, ChunkPopulateEvent event) {
		event.getWorld().getBlockAt(x, y, z).setTypeId(54);
		Inventory inventory = ((ContainerBlock)event.getWorld().getBlockAt(x, y, z).getState()).getInventory();
		int l2 = 0;
		Structure.lootChest chest = structure.chests.get(-id - structure.randoms.size() - 32);
		ArrayList<Integer> openSlots = new ArrayList<Integer>();
		for (int slots = 0; slots < inventory.getSize(); slots++) {
			openSlots.add(slots);
		}
		while (true) {
			if (l2 >= chest.numOfLoot) {
				break;
			}
			int roll = rand.nextInt(chest.totalweight);
			int total = 0;
			Structure.lootItem item = null;
			for (int loop = 0; loop < chest.loot.size(); loop++) {
				item = chest.loot.get(loop);
				if (total + item.weight > roll) {
					break;
				}
				total += item.weight;
			}
			if (item != null) {
				int slot = rand.nextInt(openSlots.size());
				inventory.setItem(openSlots.get(slot),
						new ItemStack(item.itemID, rand.nextInt(item.numberInStack) + 1, item.metadata));
				openSlots.remove(slot);
			}

			++l2;
		}
	}

	public static void generateSpawner(int x, int y, int z, int id, Random rand, Structure structure, ChunkPopulateEvent event) {
		event.getWorld().getBlockAt(x, y, z).setTypeId(52);
		Structure.Spawner spawner = structure.spawners
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
		((CreatureSpawner)event.getWorld().getBlockAt(x, y, z).getState()).setCreatureTypeId(mobID);
		
	}

}

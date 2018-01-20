package plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;

public class Plugin extends JavaPlugin {
	
	public static HashSet<Structure> structures;

	@Override
	public void onDisable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEnable() {
		structures = new HashSet<Structure>();
		loadStructures();
		getServer().getPluginManager().registerEvent(Event.Type.CHUNK_POPULATED, new GenerateStructures(), Event.Priority.High, this);
		getServer().getLogger().info("Loaded Custom Structures.");
	}
	
	public void loadStructures() {
		File structDir = getDataFolder();
		structDir.mkdirs();
		File[] structureFiles = structDir.listFiles();
		if (structureFiles == null) {
			return;
		}
		for (File file : structureFiles) {

			// Skips errors file if it exists
			if (file.getName().endsWith(".zip")){
				
				try {

					ZipFile zipfile = new ZipFile(file);
					for(Enumeration<? extends ZipEntry> entries = zipfile.entries();
					        entries.hasMoreElements();){

					Structure loadingStruct = new Structure();
					short worldType;
					ZipEntry zip = entries.nextElement();
					
					// Setup random
					loadingStruct.random = (zip.getName()+file.getName()).hashCode();
					
					// Read version
					Scanner lineReader = new Scanner(zipfile.getInputStream(zip));
					if (!lineReader.hasNextLine()) {
						error(file, "There is nothing in the file.");
						continue;
					}
					worldType = lineReader.nextShort();
					if (worldType < 1 || worldType > 3) {
						error(file, "This version of CustomStructure can only read structures of versions 1, 2, and 3.");
						continue;
					}
					loadingStruct.type = worldType;
					lineReader.nextLine();

					// Read Biome
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: Biome check.");
						continue;
					}
					String line = lineReader.nextLine();
					Scanner scnr;
					if (line.equals("x")) {
						loadingStruct.hasBiome = false;
					} else {
						scnr = new Scanner(line);
						for (int i = 0; i < 4; i++) {
							if (!scnr.hasNextDouble()) {
								error(file, "Biome detection does not contain all decimals.");
								scnr.close();
								continue;
							}
						}
						scnr.close();
						scnr = new Scanner(line);
						loadingStruct.topTemp = scnr.nextDouble();
						loadingStruct.lowTemp = scnr.nextDouble();
						loadingStruct.topHumidity = scnr.nextDouble();
						loadingStruct.lowHumidity = scnr.nextDouble();
						scnr.close();
					}

					// Read Commonality
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: Commonality.");
						continue;
					}
					scnr = new Scanner(lineReader.nextLine());
					if (!scnr.hasNextDouble()) {
						error(file, "Commonality is not a decimal");
						continue;
					}
					loadingStruct.commonality = scnr.nextDouble();
					scnr.close();

					// Read y coordinates
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: Size read.");
						continue;
					}
					scnr = new Scanner(lineReader.nextLine());
					if (scnr.hasNextShort()) {
						loadingStruct.yTop = scnr.nextShort();
					} else {
						error(file, "Y coordinates are not two integers");
						continue;
					}
					if (scnr.hasNextShort()) {
						loadingStruct.yBottom = scnr.nextShort();
					} else {
						error(file, "Y coordinates are not two integers");
						continue;
					}
					scnr.close();
					if(loadingStruct.yTop < loadingStruct.yBottom){
						short temp = loadingStruct.yTop;
						loadingStruct.yTop = loadingStruct.yBottom;
						loadingStruct.yBottom = temp;
					}

					// Read size
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: Size read.");
						continue;
					}
					scnr = new Scanner(lineReader.nextLine());
					short[] size = new short[3];
					for (int i = 0; i < 3; i++) {
						if (scnr.hasNextShort()) {
							size[i] = scnr.nextShort();
						} else {
							error(file, "Size read is not all integers or is lacking numbers");
							continue;
						}
					}
					scnr.close();

					// Read initial check
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: Initial check.");
						continue;
					}
					line = "";
					short numberOfChecks = 0;
						while (lineReader.hasNextLine()) {
							String temp = lineReader.nextLine();
							if (temp.equals("")) {
								break;
							}
							line += temp + "\n";
							numberOfChecks++;
						}
					if(numberOfChecks == 0){
						loadingStruct.hasInitial = false;
					}
					loadingStruct.initialCheck = new short[numberOfChecks][5];
					scnr = new Scanner(line);
					for (int i = 0; i < numberOfChecks; i++) {
						Scanner set = new Scanner(scnr.nextLine());
						for (int j = 0; j < 4; j++) {
							if (!set.hasNextShort()) {
								error(file, "Initial check is not all integers or is lacking numbers");
								continue;
							}
							loadingStruct.initialCheck[i][j] = set.nextShort();
						}
						if (set.hasNextShort()) {
							loadingStruct.initialCheck[i][4] = set.nextShort();
						} else {
							loadingStruct.initialCheck[i][4] = -1;
						}
						set.close();

					}
					scnr.close();

					// Read deep check
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: Deep check.");
						continue;
					}
					if (lineReader.hasNextShort()) {
						loadingStruct.deepCheck = new short[size[0]][size[1]][size[2]];
						try {
							for (int y = 0; y < size[1]; y++) {
								for (int z = 0; z < size[2]; z++) {
									for (int x = 0; x < size[0]; x++) {
										loadingStruct.deepCheck[x][y][z] = lineReader.nextShort();
									}
								}
							}
						} catch (Exception e) {
							error(file,
									"There was an error in reading the deep check. This could be from there being a non-integer object or the array of numbers is the wrong size.");
							continue;
						}
					} else {
						loadingStruct.hasDeep = false;
					}
					lineReader.nextLine();

					// Read structure
					if (!lineReader.hasNextLine()) {
						error(file, "File is incomplete. Stops at: structure read.");
						continue;
					}
					loadingStruct.structure = new short[size[0]][size[1]][size[2]];
					try {
						for (int y = 0; y < size[1]; y++) {
							for (int z = 0; z < size[2]; z++) {
								for (int x = 0; x < size[0]; x++) {
									loadingStruct.structure[x][y][z] = lineReader.nextShort();
								}
							}
						}
					} catch (Exception e) {
						error(file,
								"There was an error in reading the structure. This could be from there being a non-integer object or the array of numbers is the wrong size.");
						continue;
					}

					// Read Metadata
					if (lineReader.hasNextShort()) {
						lineReader.nextLine();
						lineReader.nextLine();
						line = lineReader.nextLine();
						numberOfChecks = 1;
						while (lineReader.hasNextLine()) {
							String temp = lineReader.nextLine();
							if (temp.equals("")) {
								break;
							}
							line += "\n" + temp;
							numberOfChecks++;
						}
						loadingStruct.metadata = new short[numberOfChecks][4];
						scnr = new Scanner(line);
						for (int i = 0; i < numberOfChecks; i++) {
							for (int j = 0; j < 4; j++) {
								if (!scnr.hasNextShort()) {
									error(file, "Metadata is not all integers or you are missing numbers");
									continue;
								}
								loadingStruct.metadata[i][j] = scnr.nextShort();
							}
	
						}
					}
					else {
						loadingStruct.hasMeta = false;
					}
					scnr.close();

					while (lineReader.hasNext()) {
						String input = lineReader.next();
						if (input.equals("chest")) {
							readChest(lineReader, loadingStruct);
						} else if (input.equals("spawner")) {
							readSpawner(lineReader, loadingStruct);
						} else if (input.equals("random")) {
							readRandom(lineReader, loadingStruct);
						} else if (input.equals("check")) {
							readMultiChecks(lineReader, loadingStruct);
						} else {
							error(file, "Attmepted to read a custom chest or random number but failed. Input was: "
									+ input);
							break;
						}
					}

					lineReader.close();
					structures.add(loadingStruct);
					}
					zipfile.close();
				} catch (FileNotFoundException e) {
					error(file, "File was detected yet not readable.");
					break;
				} catch (Exception e) {
					error(file, e.getMessage());
					break;
				}
				
			}
		}
	}
	
	public static void readMultiChecks(Scanner scnr, Structure structure) throws Exception {
		try {
			Stack<Short> inputs = new Stack<Short>();
			while (scnr.hasNextShort()) {
				inputs.push(scnr.nextShort());
			}
			short[] checks = new short[inputs.size()];
			for (int i = 0; i < checks.length; i++) {
				checks[i] = inputs.pop();
			}
			structure.multiChecks.add(checks);
		} catch (Exception e) {
			throw new Exception("Check was not able to be read.");
		}
	}

	public static void readRandom(Scanner scnr, Structure structure) throws Exception {
		try {
			while (scnr.hasNextShort()) {
				Stack<Short> IDInputs = new Stack<Short>();
				Stack<Short> weightInputs = new Stack<Short>();
				while (scnr.hasNextShort()) {
					IDInputs.push(scnr.nextShort());
					weightInputs.push(scnr.nextShort());
				}
				int[] IDs = new int[IDInputs.size()];
				int[] weights = new int[weightInputs.size()];
				int totalWeight = 0;
				for (int i = 0; i < IDs.length; i++) {
					IDs[i] = IDInputs.pop();
					weights[i] = weightInputs.pop();
					totalWeight += weights[i];
				}
				structure.createNewRandom(IDs, weights, totalWeight);
			}
		} catch (Exception e) {
			throw new Exception("Random was not able to be read.");
		}
	}

	public static void readChest(Scanner scnr, Structure structure) throws Exception {
		Structure.lootChest chest = structure.getNewChest();
		try {
			chest.numOfLoot = scnr.nextShort();
			short[] data = new short[4];
			byte loops = 0;
			while (scnr.hasNextShort()) {
				Scanner line = new Scanner(scnr.nextLine());
				while(line.hasNextShort()){
					data[loops] = line.nextShort();
					loops++;
				}
				if(loops > 3)
					chest.addLoot(data[0], data[1], data[2], data[3]);
				else {
					chest.addLoot(data[0], (short) 0, data[1], data[2]);
				}
				line.close();
				loops = 0;
			}
		} catch (Exception e) {
			throw new Exception("Chest was not able to be read.");
		}
	}

	public static void readSpawner(Scanner scnr, Structure structure) throws Exception {
		Structure.Spawner spawner = structure.getNewSpawner();
		scnr.nextLine();
		try {
			while (scnr.hasNextLine()) {
				String line = scnr.nextLine();
				if (line.equals("")) {
					break;
				}
				Scanner inner = new Scanner(line);
				spawner.mobIDs.add(inner.next());
				short weight = inner.nextShort();
				spawner.weights.add(weight);
				spawner.totalweight += weight;
				inner.close();
			}
		} catch (Exception e) {
			throw new Exception("Spawner was not able to be read.");
		}
	}

	public void error(File file, String string) {
		getServer().getLogger().info("Error at file " + file.getName());
		getServer().getLogger().info(string);
	}

}

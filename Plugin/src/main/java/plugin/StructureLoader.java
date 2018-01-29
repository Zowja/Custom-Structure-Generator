package plugin;

import plugin.structure.LootChest;
import plugin.structure.RandomNumberSet;
import plugin.structure.Spawner;
import plugin.structure.Structure;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StructureLoader {

    private final Logger logger;

    private String state = "";
    private int skipLines, neededRandomsChestsSpawners, neededChecks;

    public StructureLoader(final Logger logger) {
        this.logger = logger;
    }

    public Collection<Structure> loadFromFile(final File file) throws IOException {
        if (file.getName().endsWith(".zip")) {
            return this.loadStructurePack(new ZipFile(file));
        }

        final String name = file.getName();
        final List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
        final Structure structure = this.loadFromLines(name, lines);

        if (structure == null) return null;

        final HashSet<Structure> structures = new HashSet<>();
        structures.add(structure);
        return structures;
    }

    public Collection<Structure> loadStructurePack(final ZipFile zipFile) throws IOException {
        final HashSet<Structure> structures = new HashSet<>();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            // TODO: handle directories within zip files
            // TODO: improve name so it does not contains server relative path to file but only data folder relative
            //       so name should have format: zipFileName.zip/structFileName
            //                               or: zipFileName.zip/folderName/structFileName
            final String name = zipFile.getName() + "/" + entry.getName();
            final Scanner lineReader = new Scanner(zipFile.getInputStream(entry));
            final List<String> lines = new ArrayList<>();
            while(lineReader.hasNextLine())
                lines.add(lineReader.nextLine());
            final Structure structure = this.loadFromLines(name, lines);
            if (structure == null) continue;
            structures.add(structure);
        }
        if (structures.isEmpty()) return null;
        return structures;
    }

    public Structure loadFromString(final String name, final String data) {
        return this.loadFromLines(name, Arrays.asList(data.split("\n")));
    }

    public Structure loadFromLines(final String name, final List<String> lines) {
        // reset global variables
        this.state = "";
        this.neededChecks = 0;
        this.neededRandomsChestsSpawners = 0;
        this.skipLines = 0;

        final Structure struct = new Structure();
        struct.seed = name.hashCode();
        lines.replaceAll(String::trim); // trim all lines
        lines.replaceAll(this::stripComments); // strip all comments from line endings
        lines.replaceAll(line -> line.replaceAll("  +", " ")); // make sure all values are separated only by one space
        String readNext = "world type"; // first property to read
        for (int i = 0; i < lines.size(); i++) {
            if (this.skipLines > 0) { // skip lines if they were already read in multiline property
                this.skipLines--;
                continue;
            }
            int lineNum = i + 1;
            final String line = lines.get(i);

            // skip empty and meaningless or commented lines
            if (this.shouldBeSkipped(line) && !readNext.equalsIgnoreCase("initial check")) continue;

            switch (readNext) {
                case "world type":
                    this.readWorldType(struct, name, lineNum, line);
                    readNext = "temperature and humidity";
                    break;
                case "temperature and humidity":
                    this.readTemperatureAndHumidity(struct, name, lineNum, line);
                    readNext = "commonality";
                    break;
                case "commonality":
                    this.readCommonality(struct, name, lineNum, line);
                    readNext = "height";
                    break;
                case "height":
                    this.readHeight(struct, name, lineNum, line);
                    readNext = "size";
                    break;
                case "size":
                    this.readSize(struct, name, lineNum, line);
                    readNext = "initial check";
                    break;
                case "initial check":
                    this.readInitialCheck(struct, name, lineNum, lines);
                    readNext = "deep check";
                    break;
                case "deep check":
                    this.readDeepCheck(struct, name, lineNum, lines);
                    readNext = "structure";
                    break;
                case "structure":
                    this.readStructure(struct, name, lineNum, lines);
                    readNext = "metadata";
                    break;
                case "metadata":
                    this.readMetaData(struct, name, lineNum, lines);
                    readNext = "additionals";
                    break;
                case "additionals": // TODO: better name?
                    this.readAdditionals(struct, name, lineNum, lines);
                    break;
            }

            // stop reading if there was an error or structure is fully read
            if (this.state.equalsIgnoreCase("error") || this.state.equalsIgnoreCase("done"))
                break;

        }

        // no metadata was found and no additionals are needed, so just mark it as done
        if (readNext.equalsIgnoreCase("metadata") && this.neededChecks == 0 && this.neededRandomsChestsSpawners == 0)
            this.state = "done";

        // no additionals were found but they were not needed, so just mark it as done
        if (readNext.equalsIgnoreCase("additionals") && this.neededChecks == 0 && this.neededRandomsChestsSpawners == 0)
            this.state = "done";

        // there was an error, return null
        if (this.state.equalsIgnoreCase("error"))
            return null;

        // not all randoms/checks/chests/spawners were found
        if (!this.state.equalsIgnoreCase("done") && readNext.equalsIgnoreCase("additionals")) {
            if (this.neededChecks > 0)
                this.error(name, -1, "File is incomplete. Can't find all required multiChecks.");
            else if (this.neededRandomsChestsSpawners > 0)
                this.error(name, -1, "File is incomplete. Can't find all required randoms and/or chests and/or spawners.");
            else
                this.error(name, -1, "Theoretically impossible error.");
            return null;
        }

        // structure file was incomplete, print error and return null
        if (!this.state.equalsIgnoreCase("done")) {
            this.error(name, -1, "File is incomplete. Can't find " + readNext + " settings.");
            return null;
        }

        // structure was successfully parsed, return it
        return struct;
    }

    private void readWorldType(final Structure struct, final String name, final int lineNum, final String line) {
        try {
            struct.worldType = Short.parseShort(line);
        } catch (final NumberFormatException e) {
            this.warn(name, lineNum, "Invalid worldType. Falling back to default 0.");
            struct.worldType = 0;
        }
    }

    private void readTemperatureAndHumidity(final Structure struct, final String name, final int lineNum, final String line) {
        if (line.equalsIgnoreCase("x")) {
            return;
        }
        final String[] values = line.split(" ");
        if (values.length < 4) {
            this.warn(name, lineNum, "Temperature and humidity settings require 4 values, found less. Ignoring them.");
            return;
        }
        try {
            struct.setBiome(
                    Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]),
                    Double.parseDouble(values[2]),
                    Double.parseDouble(values[3])
            );
        } catch (final NumberFormatException e) {
            this.warn(name, lineNum, "Invalid temperature and humidity settings. Ignoring them.");
        }
    }

    private void readCommonality(final Structure struct, final String name, final int lineNum, final String line) {
        try {
            struct.commonality = Double.parseDouble(line);
            if (struct.commonality == 0)
                this.warn(name, lineNum, "Commonality value can't be 0. Structure will not be generated.");
        } catch (final NumberFormatException e) {
            this.warn(name, lineNum, "Invalid commonality value. Structure will not be generated.");
            struct.commonality = 0;
        }
    }

    private void readHeight(final Structure struct, final String name, final int lineNum, final String line) {
        final String[] values = line.split(" ");
        if (values.length < 2) {
            this.warn(name, lineNum, "Height settings require 2 values, found less. Default values will be used.");
            return;
        }
        try {
            struct.setHeightLimit(Short.parseShort(values[0]), Short.parseShort(values[1]));
        } catch (final NumberFormatException e) {
            this.warn(name, lineNum, "Invalid height value. It has to be two integers. Default values will be used.");
        }
    }

    private void readSize(final Structure struct, final String name, final int lineNum, final String line) {
        final short[] size = new short[3];
        final String[] values = line.split(" ");
        if (values.length < 3) {
            this.error(name, lineNum, "Structure size require 3 values, found less.");
            this.state = "error";
            return;
        }
        try {
            for (int i = 0; i < 3; i++) {
                size[i] = Short.parseShort(values[i]);
                if (size[i] <= 0) {
                    this.error(name, lineNum, "Structure size values have to be positive (more than 0).");
                    this.state = "error";
                       return;
                }
            }
        } catch (final NumberFormatException e) {
            this.error(name, lineNum, "Invalid structure size values.");
            this.state = "error";
            return;
        }
        struct.structure = new short[size[0]][size[1]][size[2]];
    }

    private void readInitialCheck(final Structure struct, final String name, final int lineNum, final List<String> lines) {
        int checkLineNum = lineNum;
        final List<short[]> checks = new ArrayList<>();
        while (!lines.get(checkLineNum-1).isEmpty()) {
            final String line = lines.get(checkLineNum-1);
            checkLineNum++;
            this.skipLines++;
            if (this.shouldBeSkipped(line)) continue; // skip commented lines
            final String[] values = line.split(" ");
            if (values.length < 4) {
                this.warn(name, lineNum, "Initial check require at least 4 values. Found less. Ignoring check.");
                continue;
            }
            try {
                final short[] check = new short[5];
                for (int i = 0; i < 4; i++)
                    check[i] = Short.parseShort(values[i]);
                if (values.length > 4)
                    check[4] = Short.parseShort(values[4]);
                else
                    check[4] = -1;
                if (check[3] < -31) this.neededChecks++; // TODO: probably should be moved outside readInitialCheck method
                checks.add(check);
            } catch (final NumberFormatException e) {
                this.warn(name, lineNum, "Invalid initial check values. Ignoring check.");
                continue;
            }
        }
        if (!checks.isEmpty())
            struct.initialCheck = (short[][]) checks.toArray();
    }

    private void readDeepCheck(final Structure struct, final String name, int lineNum, final List<String> lines) {
        if (lines.get(lineNum-1).equalsIgnoreCase("x")) return;
        struct.deepCheck = struct.structure.clone();
        for (int height = 0; height < struct.deepCheck[0].length; height++) {
            for (int length = 0; length < struct.deepCheck[0][0].length; length++) {
                final String line = lines.get(lineNum-1);
                if (this.shouldBeSkipped(line)) { // skip empty and commented lines
                    length--;
                } else {
                    final String[] values = lines.get(lineNum-1).split(" ");
                    if (values.length < struct.deepCheck.length) {
                        this.error(name, lineNum, "Deep check is missing some values.");
                        this.state = "error";
                        return;
                    }
                    for (int width = 0; width < struct.deepCheck.length; width++) {
                        try {
                            struct.deepCheck[width][height][length] = Short.parseShort(lines.get(lineNum-1));
                            if (struct.deepCheck[width][height][length] < -31) this.neededChecks++; // TODO: probably should be moved outside readDeepCheck method
                        } catch (final NumberFormatException e) {
                            this.error(name, lineNum, "Invalid deep check value.");
                            this.state = "error";
                            return;
                        }
                    }
                }
                lineNum++;
                this.skipLines++;
            }
        }
    }

    private void readStructure(final Structure struct, final String name, int lineNum, final List<String> lines) {
        for (int height = 0; height < struct.structure[0].length; height++) {
            for (int length = 0; length < struct.structure[0][0].length; length++) {
                final String line = lines.get(lineNum-1);
                if (this.shouldBeSkipped(line)) { // skip empty and commented lines
                    length--;
                } else {
                    final String[] values = line.split(" ");
                    if (values.length < struct.structure.length) {
                        this.error(name, lineNum, "Structure is missing some blocks.");
                        this.state = "error";
                        return;
                    }
                    for (int width = 0; width < struct.structure.length; width++) {
                        try {
                            struct.structure[width][height][length] = Short.parseShort(values[width]);
                            if (struct.structure[width][height][length] < -31) {
                                this.neededRandomsChestsSpawners++; // TODO: probably should be moved outside readStructure method
                            }
                        } catch (final NumberFormatException e) {
                            this.error(name, lineNum, "Invalid structure block id.");
                            this.state = "error";
                            return;
                        }
                    }
                }
                lineNum++;
                this.skipLines++;
            }
        }
    }

    private void readMetaData(final Structure struct, final String name, int lineNum, final List<String> lines) {
        int metaLineNum = lineNum;
        final List<short[]> checks = new ArrayList<>();
        while (!lines.get(metaLineNum-1).isEmpty()) {
            final String line = lines.get(metaLineNum-1);
            if (this.shouldBeSkipped(line)) continue; // skip commented lines
            final String[] values = line.split(" ");
            if (values.length < 4) {
                this.warn(name, lineNum, "Metadata check require at 4 values. Found less. Ignoring check.");
                continue;
            }
            try {
                final short[] check = new short[4];
                for (int v = 0; v < 4; v++)
                    check[v] = Short.parseShort(values[v]);
                checks.add(check);
            } catch (final NumberFormatException e) {
                this.warn(name, lineNum, "Invalid initial check values. Ignoring check.");
                continue;
            }
            metaLineNum++;
            this.skipLines++;
        }
        if (!checks.isEmpty()) {
            short[][] meta = new short[checks.size()][4];
            for (int i = 0; i < checks.size(); i++) {
                meta[i][0] = checks.get(0)[0];
                meta[i][1] = checks.get(0)[1];
                meta[i][2] = checks.get(0)[2];
                meta[i][3] = checks.get(0)[3];
            }
            struct.metadata = meta;
        }
    }

    private void readAdditionals(final Structure struct, final String name, int lineNum, final List<String> lines) {
        while (lines.size() - lineNum > 0 && (this.neededChecks > 0 || this.neededRandomsChestsSpawners > 0)) {
            final String line = lines.get(lineNum-1);
            if (this.shouldBeSkipped(line)) { // skip empty and commented lines
                lineNum++;
                continue;
            }

            // TODO: extract to separate method
            if (line.equalsIgnoreCase("check") && this.neededChecks > 0) {
                int checkLineNum = 1;
                final Stack<Short> values = new Stack<>();
                while (!lines.get(lineNum-1+checkLineNum).isEmpty()) {
                    final String checkLine = lines.get(lineNum-1+checkLineNum);
                    if (!this.shouldBeSkipped(checkLine)) { // line is not commented
                        try {
                            values.push(Short.parseShort(checkLine));
                        } catch (final NumberFormatException e) {
                            this.warn(name, lineNum+checkLineNum, "Invalid value when reading multiCheck. Skipping that value.");
                        }
                    }
                    checkLineNum++;
                }
                if (values.empty()) {
                    this.warn(name, lineNum, "multiCheck is empty. Ignoring it.");
                } else {
                    short[] check = new short[values.size()];
                    for (int i=0; i < values.size(); i++)
                        check[i] = values.pop();
                    struct.multiChecks.add(check);
                    this.neededChecks--;
                }
                lineNum += checkLineNum;
                continue;
            }

            // TODO: extract to separate method
            if (line.equalsIgnoreCase("random") && this.neededRandomsChestsSpawners > 0) {
                int randomLineNum = 1;
                final RandomNumberSet random = new RandomNumberSet();
                while (!lines.get(lineNum-1+randomLineNum).isEmpty()) {
                    final String randomLine = lines.get(lineNum-1+randomLineNum);
                    if (!this.shouldBeSkipped(randomLine)) { // line is not commented
                        final String[] values  = randomLine.split(" ");
                        try {
                            final short number = Short.parseShort(values[0]);
                            final short weight = values.length > 1 ? Short.parseShort(values[1]) : 1;
                            random.addNumber(number, weight);
                        } catch (final NumberFormatException e) {
                            this.warn(name, lineNum+randomLineNum, "Invalid value when reading random. Skipping that value.");
                        }
                    }
                    randomLineNum++;
                }
                if (random.hasNumbers()) {
                    this.warn(name, lineNum, "random is empty. Ignoring it.");
                } else {
                    struct.randoms.add(random);
                    this.neededRandomsChestsSpawners--;
                }
                lineNum += randomLineNum;
                continue;
            }

            // TODO: extract to separate method
            if (line.startsWith("chest")  && this.neededRandomsChestsSpawners > 0) {
                short amount;
                try {
                    amount = Short.parseShort(line.split(" ")[1]);
                } catch (final Exception e) {
                    this.warn(name, lineNum, "Invalid amount value in chest declaration. Skipping that chest.");
                    ++lineNum;
                    continue;
                }
                if (amount <= 0) {
                    this.warn(name, lineNum, "Amount value in chest declaration must be more than 0. Skipping that chest.");
                    ++lineNum;
                    continue;
                }
                if (amount > 27) {
                    this.warn(name, lineNum, "Amount value in chest declaration is to large. Falling back to 27 (max value).");
                    amount = 27;
                }
                final LootChest chest = new LootChest();
                chest.numOfLoot = amount;
                int chestLineNum = 1;
                while (!lines.get(lineNum-1+chestLineNum).isEmpty()) {
                    final String lootEntryLine = lines.get(lineNum-1+chestLineNum);
                    if (!this.shouldBeSkipped(lootEntryLine)) { // line is not commented
                        final String[] values  = lootEntryLine.split(" ");
                        if (values.length < 3) {
                            this.warn(name, lineNum+chestLineNum, "Loot entry requires at least 3 values. Found less. Skipping that entry.");
                            chestLineNum++;
                            continue;
                        }
                        try {
                            final short[] lootData = new short[4];
                            lootData[0] = Short.parseShort(values[0]);
                            lootData[1] = values.length == 3 ? 0 : Short.parseShort(values[1]);
                            lootData[2] = values.length == 3 ? Short.parseShort(values[1]) : Short.parseShort(values[2]);
                            lootData[3] = values.length == 3 ? Short.parseShort(values[2]) : Short.parseShort(values[3]);
                            chest.addLoot(lootData[0], lootData[1], lootData[2], lootData[3]);
                        } catch (final NumberFormatException e) {
                            this.warn(name, lineNum+chestLineNum, "Invalid value when reading loot entry. Skipping that entry.");
                        }
                    }
                    chestLineNum++;
                }
                if (chest.hasLoot()) {
                    this.warn(name, lineNum, "Chest is empty. Skipping it.");
                } else {
                    struct.chests.add(chest);
                }
                lineNum += chestLineNum;
                continue;
            }

            // TODO: extract to separate method
            if (line.startsWith("spawner")  && this.neededRandomsChestsSpawners > 0) {
                int spawnerLineNum = 1;
                final Spawner spawner = new Spawner();
                while (!lines.get(lineNum-1+spawnerLineNum).isEmpty()) {
                    final String spawnerLine = lines.get(lineNum-1+spawnerLineNum);
                    if (!this.shouldBeSkipped(spawnerLine)) { // line is not commented
                        final String[] values  = spawnerLine.split(" ");
                        try {
                            final String mobId = values[0];
                            final short weight = values.length > 1 ? Short.parseShort(values[1]) : 1;
                            spawner.addEntry(mobId, weight);
                        } catch (final NumberFormatException e) {
                            this.warn(name, lineNum+spawnerLineNum, "Invalid value when reading spawner entry. Skipping that entry.");
                        }
                    }
                    spawnerLineNum++;
                }
                if (!spawner.hasEntries()) {
                    this.warn(name, lineNum, "Spawner is empty. Ignoring it.");
                } else {
                    struct.spawners.add(spawner);
                    this.neededRandomsChestsSpawners--;
                }
                lineNum += spawnerLineNum;
                continue;

            }

            lineNum++;
        }
        if (this.neededChecks == 0 && this.neededRandomsChestsSpawners == 0) {
            this.state = "done";
        } else {
            this.state = "error";
            if (this.neededChecks > 0)
                this.error(name, -1, "Missing " + this.neededChecks + " multiChecks.");
            if (this.neededRandomsChestsSpawners > 0) {
                this.error(name, -1, "Missing " + this.neededRandomsChestsSpawners + " randoms and/or chests and/or spawners.");
            }
        }

    }

    /** Checks if line is commented or empty */
    private boolean shouldBeSkipped(final String line) {
        return line.startsWith("#") || line.isEmpty();
    }

    /** Removes comments from line endings */
    private String stripComments(final String line) {
        if (!line.startsWith("#") && line.contains("#"))
            return line.split("#")[0].trim();
        return line;
    }

    private void warn(final String name, final int lineNum, final String message) {
        this.logger.warning("[CS] Error parsing '" + name + "' structure. " + message + " (line " + lineNum + ")");
    }

    private void error(final String name, final int lineNum, final String message) {
        if (lineNum == -1)
            this.logger.warning("[CS][!] Failed to parse '" + name + "' structure. " + message);
        else
            this.logger.warning("[CS][!] Failed to parse '" + name + "' structure. " + message + " (line " + lineNum + ")");
    }

}

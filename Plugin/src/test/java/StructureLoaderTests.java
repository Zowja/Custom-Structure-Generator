import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import io.github.zowja.StructureLoader;
import io.github.zowja.structure.Structure;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class StructureLoaderTests {

    @Test
    void testBasicValidStructure() {
        final List<String> lines = Arrays.asList(
                "1",
                "x",
                "10",
                "64 128",
                "1 1 1",
                "",
                "x",
                "",
                "1"
        );
        final StructureLoader loader = new StructureLoader(Logger.getLogger("test"));
        final Structure struct = loader.loadFromLines("testStructure", lines);
        assertNotNull(struct);
        assertEquals(1, struct.worldType);
        assertFalse(struct.hasBiome());
        assertEquals(10, struct.commonality);
        assertEquals(64, struct.getHeightLimitMin());
        assertEquals(128, struct.getHeightLimitMax());
        assertEquals(1, struct.getWidth());
        assertEquals(1, struct.getHeight());
        assertEquals(1, struct.getLength());
        assertFalse(struct.hasInitial());
        assertFalse(struct.hasDeep());
        assertEquals(1, struct.structure[0][0][0]);
        assertFalse(struct.hasMeta());
    }

    @Test
    void testBasicValidStructureWithComments() {
        final List<String> lines = Arrays.asList(
                "# cool test stone block",
                "# by Zowja",
                "1",
                "x # there would be biome check",
                "10",
                "64 128 # minHeight maxHeight",
                "1 1 1",
                "# there would be initial check but it is skipped",
                "",
                "# x instead of deep check",
                "x",
                "",
                "1 # coolest stone in the universe",
                "# end of the file"
        );
        final StructureLoader loader = new StructureLoader(Logger.getLogger("test"));
        final Structure struct = loader.loadFromLines("testStructure", lines);
        assertNotNull(struct);
        assertEquals(1, struct.worldType);
        assertFalse(struct.hasBiome());
        assertEquals(10, struct.commonality);
        assertEquals(64, struct.getHeightLimitMin());
        assertEquals(128, struct.getHeightLimitMax());
        assertEquals(1, struct.getWidth());
        assertEquals(1, struct.getHeight());
        assertEquals(1, struct.getLength());
        assertFalse(struct.hasInitial());
        assertFalse(struct.hasDeep());
        assertEquals(1, struct.structure[0][0][0]);
        assertFalse(struct.hasMeta());
    }

    @Test
    void testBasicValidStructureWithAdditionalSpaces() {
        final List<String> lines = Arrays.asList(
                "1   ",
                " x ",
                "10",
                "64    128",
                "  1  1      1   ",
                "",
                "x       ",
                "                                                 ",
                "                     1"
        );
        final StructureLoader loader = new StructureLoader(Logger.getLogger("test"));
        final Structure struct = loader.loadFromLines("testStructure", lines);
        assertNotNull(struct);
        assertEquals(1, struct.worldType);
        assertFalse(struct.hasBiome());
        assertEquals(10, struct.commonality);
        assertEquals(64, struct.getHeightLimitMin());
        assertEquals(128, struct.getHeightLimitMax());
        assertEquals(1, struct.getWidth());
        assertEquals(1, struct.getHeight());
        assertEquals(1, struct.getLength());
        assertFalse(struct.hasInitial());
        assertFalse(struct.hasDeep());
        assertEquals(1, struct.structure[0][0][0]);
        assertFalse(struct.hasMeta());
    }

    @Test
    void testValidStructureLoadedFromFile() {
        final String name = "bush";
        final URL url = this.getClass().getResource(name);
        final File structFile = new File(url.getFile());
        final StructureLoader loader = new StructureLoader(Logger.getLogger("test"));
        final Collection<Structure> structs = loader.loadFromFile(structFile);
        assertNotNull(structs);
        assertEquals(1, structs.size());
        final Structure struct = structs.toArray(new Structure[0])[0];
        assertNotNull(struct);
        assertEquals(1, struct.worldType);
        assertTrue(struct.hasBiome());
        assertEquals(90, struct.getMinTemperature());
        assertEquals(100, struct.getMaxTemperature());
        assertEquals(0, struct.getMinHumidity());
        assertEquals(40, struct.getMaxHumidity());
        assertEquals(10, struct.commonality);
        assertEquals(64, struct.getHeightLimitMin());
        assertEquals(80, struct.getHeightLimitMax());
        assertEquals(3, struct.getWidth());
        assertEquals(4, struct.getHeight());
        assertEquals(3, struct.getLength());
        assertTrue(struct.hasInitial());
        assertEquals(-32,struct.initialCheck[0][3]);
        assertEquals(0,struct.initialCheck[1][3]);
        assertEquals(0,struct.initialCheck[2][3]);
        assertFalse(struct.hasDeep());
        assertEquals(17, struct.structure[1][1][1]);
        assertEquals(-32, struct.structure[0][3][2]);
        assertFalse(struct.hasMeta());
        assertEquals(1, struct.multiChecks.size());
        assertEquals(3, struct.multiChecks.get(0).length);
        assertEquals(3, struct.multiChecks.get(0)[0]);
        assertEquals(2, struct.multiChecks.get(0)[1]);
        assertEquals(12, struct.multiChecks.get(0)[2]);
        assertEquals(1, struct.randoms.size());
        assertTrue(struct.randoms.get(0).hasNumbers());
    }


}

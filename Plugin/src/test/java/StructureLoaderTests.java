import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import io.github.zowja.StructureLoader;
import io.github.zowja.structure.Structure;

import java.util.Arrays;
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
                "# x insteed of deep check",
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

}

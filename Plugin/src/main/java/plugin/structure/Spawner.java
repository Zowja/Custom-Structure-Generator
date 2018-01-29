package plugin.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Spawner {

    private final List<SpawnerEntry> entries = new ArrayList<>();
    private int totalweight;

    public void addEntry(final String mobId, short weight) {
        this.entries.add(new SpawnerEntry(mobId, weight));
        this.totalweight += weight;
    }

    public boolean hasEntries() {
        return this.entries.isEmpty();
    }

    public String getMobId(final Random rand) {
        final int roll = rand.nextInt(this.totalweight) + 1;
        int total = 0;
        String mobId = null;
        for (final SpawnerEntry entry : this.entries) {
            mobId = entry.mobId;
            if (total + entry.weight > roll)
                break;
            total += entry.weight;
        }
        return mobId;
    }

    private class SpawnerEntry {
        String mobId;
        short weight;
        SpawnerEntry(final String mobId, final short weight) {
            this.mobId = mobId;
            this.weight = weight;
        }
    }

}

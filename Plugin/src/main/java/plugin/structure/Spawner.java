package plugin.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Spawner {
    public final List<String> mobIDs = new ArrayList<>();
    public final List<Short> weights = new ArrayList<>();
    public int totalweight;

    public String getMobId(final Random rand) {
        final int roll = rand.nextInt(this.totalweight) + 1;
        int total = 0;
        String mobId = null;
        for (int i = 0; i < this.mobIDs.size(); i++) {
            mobId = this.mobIDs.get(i);
            if (total + this.weights.get(i) > roll)
                break;
            total += this.weights.get(i);
        }
        return mobId;
    }

}

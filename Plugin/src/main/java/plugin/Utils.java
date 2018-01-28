package plugin;

import net.minecraft.server.NoiseGeneratorOctaves2;

import java.util.Random;

public class Utils {

    public static double getHumidity(final long seed, final int x, final int z) {
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

}

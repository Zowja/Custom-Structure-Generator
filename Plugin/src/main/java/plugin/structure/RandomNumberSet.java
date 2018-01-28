package plugin.structure;

import java.util.Arrays;

public class RandomNumberSet {

    public final int[] weight;
    public final int[] number;
    public final int totalRandomWeight;

    RandomNumberSet(final int[] weight, final int[] number){
        this.weight = weight;
        this.number = number;
        this.totalRandomWeight = Arrays.stream(weight).sum();
    }

}

package plugin.structure;

import java.util.Arrays;
import java.util.Random;

public class RandomNumberSet {

    public final int[] weight;
    public final int[] number;
    public final int totalRandomWeight;

    RandomNumberSet(final int[] weight, final int[] number){
        this.weight = weight;
        this.number = number;
        this.totalRandomWeight = Arrays.stream(weight).sum();
    }

    public int getNumber(final Random rand) {
        final int roll = rand.nextInt(this.totalRandomWeight) + 1;
        int total = 0;
        int number = 0;
        for (int i = 0; i < this.number.length; i++) {
            number = this.number[i];
            if (total + this.weight[i] > roll)
                break;
            total += this.weight[i];
        }
        return number;
    }

}

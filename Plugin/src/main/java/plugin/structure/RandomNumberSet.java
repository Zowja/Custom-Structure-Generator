package plugin.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomNumberSet {

    private final List<RandomNumber> numbers = new ArrayList<>();
    private int totalRandomWeight;

    public void addNumber(final int number, final int weight) {
        this.numbers.add(new RandomNumber(number,weight));
        this.totalRandomWeight += weight;
    }

    public int getNumber(final Random rand) {
        final int roll = rand.nextInt(this.totalRandomWeight) + 1;
        int total = 0;
        int number = 0;
        for (final RandomNumber randNum : this.numbers) {
            number = randNum.value;
            if (total + randNum.weight > roll)
                break;
            total += randNum.weight;
        }
        return number;
    }

    public boolean hasNumbers() {
        return this.numbers.isEmpty();
    }

    private class RandomNumber {
        int value;
        int weight;
        RandomNumber(final int value, final int weight) {
            this.value = value;
            this.weight = weight;
        }
    }

}

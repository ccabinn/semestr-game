package game.zombies;

import java.util.ArrayList;
import java.util.List;

public class ZombieWaveFactory {

    public static List<Zombie> createWaveForDay(int day) {
        List<Zombie> wave = new ArrayList<>();
        if (day < 1) {
            return wave;
        }


        int baseCount = 1 + (day / 2);

        for (int i = 1; i <= baseCount; i++) {
            if (day >= 10 && i % 3 == 0) {
                // Těžký zombík od 10. dne
                wave.add(new Zombie("Obrněný Zombie #" + i, 60 + (day * 3), 20 + day));
            } else if (day >= 5 && i % 2 == 0) {
                // Rychlý běžec od 5. dne
                wave.add(new Zombie("Běžec #" + i, 30 + (day * 2), 12 + day));
            } else {
                // Základní ploužící se zombie
                wave.add(new Zombie("Chodec #" + i, 20 + (day * 2), 8 + (day / 2)));
            }
        }

        return wave;
    }
}
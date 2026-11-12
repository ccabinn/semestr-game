package zombies;

import java.util.ArrayList;
import java.util.List;

public class ZombieWaveFactory {

    public static List<Zombie> createWaveForDay(int day) {
        List<Zombie> wave = new ArrayList<>();
        if (day < 10) {
            return wave;
        }


        int baseCount = 1 + ((day - 10) / 2);

        for (int i = 1; i <= baseCount; i++) {
            int waveDay = day - 10; // Počítáme jen dny od začátku invaze

            if (day >= 15 && i % 3 == 0) { // Obrněný zombie až od 15. dne
                wave.add(new Zombie("Obrněný Zombie #" + i, 40 + (waveDay * 3), 15 + waveDay));

            } else if (day >= 12 && i % 2 == 0) { // Běžec od 12. dne
                wave.add(new Zombie("Běžec #" + i, 25 + (waveDay * 2), 10 + waveDay));

            } else { // Základní chodec
                wave.add(new Zombie("Chodec #" + i, 15 + (waveDay * 2), 5 + waveDay));
            }
        }

        return wave;
    }
}

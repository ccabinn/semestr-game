package weather;

import java.util.Random;

public class WeatherManager {
    private WeatherType currentWeather = WeatherType.NORMAL;
    private int currentDisasterChance = 15; // pocatecni sance na pohromu
    private int cooldownDays = 0;       // pocet dni klidu po pohrome
    private final Random random = new Random();

    // posun na novy den
    public String advanceWeather(int currentDay) {
        // prvnich pet dni bude normalni pocasi
        if (currentDay <= 5) {
            currentWeather = WeatherType.NORMAL;
            return "Počasí: " + WeatherType.NORMAL.getDescription();
        }

        // garance klidu po pohrome
        if (cooldownDays > 0) {
            cooldownDays--;
            currentWeather = WeatherType.NORMAL;
            return "Počasí: " + WeatherType.NORMAL.getDescription() + " (klid po pohromě)";
        }

        // vyhodnoceni sance na pohromu
        int roll = random.nextInt(100);
        if (roll < currentDisasterChance) {
            currentWeather = random.nextBoolean() ? WeatherType.DROUGHT : WeatherType.FLOOD;
            currentDisasterChance = 15; // reset sance
            cooldownDays = 3;       // nastaveni cooldownu
            return "EXTRÉMNÍ POČASÍ: " + currentWeather.getDescription();
        } else {
            currentWeather = WeatherType.NORMAL;
            currentDisasterChance = Math.min(90, currentDisasterChance + 10); // narust sance o 10%
            return "Počasí: " + WeatherType.NORMAL.getDescription();
        }
    }

    public WeatherType getCurrentWeather() {
        return currentWeather;
    }

    public boolean isDrought() {
        return currentWeather == WeatherType.DROUGHT;
    }

    public boolean isFlooded() {
        return currentWeather == WeatherType.FLOOD;
    }
}
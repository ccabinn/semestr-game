package weather;

import java.util.Random;

public class WeatherManager {
    private WeatherType currentWeather = WeatherType.NORMAL;

    private int currentDisasterChance = 15; // pocatecni sance na pohromu
    private int cooldownDays = 0;           // pocet dni klidu po pohrome
    private int disasterDaysLeft = 0;       // dny do konce pohromy

    private static final int DISASTER_DURATION_DAYS = 3;
    private static final int COOLDOWN_DURATION_DAYS = 3;

    private final Random random = new Random();
    private int wellLevel = 0;

    // posun na novy den
    public String advanceWeather(int currentDay) {
        // prvnich pet dni bude normalni pocasi
        if (currentDay <= 5) {
            currentWeather = WeatherType.NORMAL;
            return "Počasí: " + WeatherType.NORMAL.getDescription();
        }

        // reseni katastrofy
        if (disasterDaysLeft > 0) {
            disasterDaysLeft--;

            if (disasterDaysLeft > 0) {
                // stale probiha
                return "EXTRÉMNÍ POČASÍ POKRAČUJE: " + currentWeather.getDescription() +
                        " (Zbývá " + disasterDaysLeft + " " + (disasterDaysLeft == 1 ? "den" : "dní") + ")";
            } else {
                // dnes skoncila, zitra zacina cooldown
                currentWeather = WeatherType.NORMAL;
                cooldownDays = COOLDOWN_DURATION_DAYS;
                return "Počasí: " + WeatherType.NORMAL.getDescription() + " (Klid po pohromě)";
            }
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
            disasterDaysLeft = DISASTER_DURATION_DAYS - 1;

            return "EXTRÉMNÍ POČASÍ: " + currentWeather.getDescription() +
                    " (Bude trvat celkem " + DISASTER_DURATION_DAYS + " " + (DISASTER_DURATION_DAYS == 1 ? "den" : (DISASTER_DURATION_DAYS > 4 ? "dní" : "dny")) + ")";
        } else {
            currentWeather = WeatherType.NORMAL;
            currentDisasterChance = Math.min(90, currentDisasterChance + 10); // narust sance o 10%
            return "Počasí: " + WeatherType.NORMAL.getDescription();
        }
    }

    public void setCurrentWeather(WeatherType weatherType) {
        if (weatherType != null) {
            this.currentWeather = weatherType;
            if (weatherType != WeatherType.NORMAL) {
                this.disasterDaysLeft = Math.max(0, DISASTER_DURATION_DAYS - 1);
                this.cooldownDays = 0;
            } else {
                this.disasterDaysLeft = 0;
            }
        }
    }

    // pouze pro nacteni ulozene hry
    public void restoreState(WeatherType weather, int disasterChance, int cooldownDays, int wellLevel) {
        this.currentWeather = weather;
        this.currentDisasterChance = Math.max(0, Math.min(90, disasterChance));
        this.cooldownDays = Math.max(0, cooldownDays);
        this.wellLevel = Math.max(0, wellLevel);
    }

    public int getDisasterChance() {
        return currentDisasterChance;
    }

    public int getCooldownDays() {
        return cooldownDays;
    }

    public void addWellLevel() {
        this.wellLevel++;
    }

    public int getWellLevel() {
        return wellLevel;
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
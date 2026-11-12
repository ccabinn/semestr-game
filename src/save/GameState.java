package save;

import java.util.List;
import java.util.Map;

// "Fotka" rozehrane hry, kterou ukladame do souboru (GDD kap. 8).
// Jsou tu jen jednoduche hodnoty (cisla, texty, ID), zadne herni objekty,
// takze ji Gson umi primo prevest na JSON a zpet.
// Record = nemenna trida: private final pole + gettery se vygeneruji samy.
public record GameState(
        int formatVersion,          // verze formatu souboru, kdybychom ho pozdeji menili
        int day,
        String locationId,
        int energy,
        int money,
        String vehicle,             // nazev TransportType, napr. "CAR"
        boolean engineTuning,
        List<String> plantedVegetables,
        Map<String, Integer> inventory,
        List<PlotState> plots,
        int sprinklerCount,
        WeatherState weather,
        int dogFoodDaysLeft,
        List<String> questStates    // radky z NpcManager.saveStates(), napr. "elena:1=ACTIVE"
) {
    public static final int CURRENT_VERSION = 1;

    // stav jednoho zahonku; cropId == null znamena prazdny zahonek
    public record PlotState(
            String cropId,
            int growthDays,
            boolean wateredToday,
            boolean wateredYesterday,
            boolean withered
    ) {}

    // stav pocasi (nahodny generator se neuklada, neni to potreba)
    public record WeatherState(
            String type,            // nazev WeatherType, napr. "DROUGHT"
            int disasterChance,
            int cooldownDays,
            int wellLevel
    ) {}
}

package map;

import loader.MapLoader;
import player.Player;

import java.util.Collections;
import java.util.Map;

public class GamePlan {

    private final Map<String, Location> allLocations;

    public GamePlan() {
        this.allLocations = MapLoader.loadMapFromJSON("/data/locations.json");
    }

    public String movePlayer(Player player, String destinationId) {
        if (player == null || player.getCurrentLocation() == null) {
            return "Chyba: Hráč nebo jeho aktuální pozice nebyla nalezena.";
        }

        Location currentLoc = player.getCurrentLocation();
        Passage passage = currentLoc.getPassage(destinationId);

        if (passage == null) {
            return "Tamtud se odsud jít nedá!";
        }

        // kontrola dopravniho prostredku
        if (player.getBestVehicle().getLevel() < passage.getRequiredTransport().getLevel()) {
            return "Do lokality " + passage.getDestination().getName()
                    + " se s tvým dopravním prostředkem nedostaneš. Vyžaduje: "
                    + passage.getRequiredTransport().getDisplayName();
        }

        // kontrola energie
        int requiredEnergy = passage.getEnergyCost(player.getBestVehicle(), player.hasEngineTuning());
        if (player.getEnergy() < requiredEnergy) {
            return "Nemáš dostatek energie na cestu! Potřebuješ " + requiredEnergy + " EN, ale máš jen " + player.getEnergy() + " EN.";
        }

        // pohyb a odecteni energie
        player.removeEnergy(requiredEnergy);
        player.setCurrentLocation(passage.getDestination());

        return "Přesunul ses do: " + passage.getDestination().getName()
                + " (Spotřebováno " + requiredEnergy + " energie).";
    }

    public Location getLocationById(String id) {
        if (id == null) return null;
        return allLocations.get(id.toLowerCase());
    }

    public Map<String, Location> getAllLocations() {
        return Collections.unmodifiableMap(allLocations);
    }
}
package map;

import loader.MapLoader;
import java.util.Map;

public class GamePlan {

    // bude v playerovi
    private Location currentLocation;
    private Map<String, Location> allLocations;
    private int energy = 100;
    private TransportType bestVehicle = TransportType.BIKE;
    private boolean hasEngineTuning = false;

    public GamePlan() {
        allLocations = MapLoader.loadMapFromJSON("/data/locations.json");
        currentLocation = allLocations.get("home");
    }

    public String goToLocation(String destinationName) {
        if (currentLocation == null) {
            return "Chyba: Aktuální lokalita není nastavena.";
        }

        Passage passage = currentLocation.getPassage(destinationName);

        if (passage == null) {
            return "Do lokality se odtud nedostaneš!";
        }

        if (bestVehicle.getLevel() < passage.getRequiredTransport().getLevel()) {
            return "Do lokality " + destinationName + " se s tvým dopravním prostředkem nedostaneš. Vyžaduje: "
                    + passage.getRequiredTransport();
        }

        int requiredEnergy = passage.getEnergyCost(bestVehicle, hasEngineTuning);
        if (energy < requiredEnergy) {
            return "Nemáš dostatek energie na cestu! Potřebuješ " + requiredEnergy + " EN, ale máš jen " + energy + " EN.";
        }

        energy -= requiredEnergy;
        currentLocation = passage.getDestination();

        return "Přesunul ses do: " + currentLocation.getName() + " (Spotřebováno " + requiredEnergy + " energie).";
    }


    // toto se hodi do playera

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.min(100, energy);
    }

    public TransportType getBestVehicle() {
        return bestVehicle;
    }

    public void setBestVehicle(TransportType bestVehicle) {
        this.bestVehicle = bestVehicle;
    }

    public boolean hasEngineTuning() {
        return hasEngineTuning;
    }

    public void setHasEngineTuning(boolean hasEngineTuning) {
        this.hasEngineTuning = hasEngineTuning;
    }
}
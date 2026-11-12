package player;

import inventory.Inventory;
import inventory.ItemCatalog;
import map.Location;
import map.TransportType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Player {

    private Location currentLocation;
    private int energy = 100;
    private int money = 20;
    private TransportType bestVehicle = TransportType.BIKE;
    private boolean hasEngineTuning = false;
    private final Inventory inventory;
    private final Set<String> plantedVegetables = new HashSet<>();

    public Player(Location startLocation, ItemCatalog itemCatalog) {
        this.currentLocation = startLocation;
        this.inventory = new Inventory(itemCatalog);
    }

    // kontrola zasazene zeleniny
    public void registerPlantedCrop(String cropId, boolean isVegetable) {
        if (isVegetable && cropId != null) {
            plantedVegetables.add(cropId.toLowerCase());
        }
    }

    // odemceni sazeni ovoce
    public boolean canPlantFruit() {
        return plantedVegetables.size() >= 8;
    }

    public int getPlantedVegetablesCount() {
        return plantedVegetables.size();
    }

    public Set<String> getPlantedVegetables() {
        return Collections.unmodifiableSet(plantedVegetables);
    }

    // pouze pro nacteni ulozene hry - nastavi hodnoty primo (s kontrolou rozsahu)
    public void restoreState(int energy, int money, TransportType bestVehicle,
                             boolean hasEngineTuning, Collection<String> plantedVegetables) {
        setEnergy(energy);
        this.money = Math.max(0, money);
        this.bestVehicle = bestVehicle;
        this.hasEngineTuning = hasEngineTuning;
        this.plantedVegetables.clear();
        this.plantedVegetables.addAll(plantedVegetables);
    }

    public void addEnergy(int amount) {
        if (amount > 0) {
            this.energy = Math.min(100, this.energy + amount);
        }
    }

    public void removeEnergy(int amount) {
        if (amount > 0) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }

    public void addMoney(int amount) {
        if (amount > 0) {
            this.money += amount;
        }
    }

    public boolean removeMoney(int amount) {
        if (amount <= 0) return false;
        if (this.money >= amount) {
            this.money -= amount;
            return true;
        }
        return false;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.min(100, Math.max(0, energy));
    }

    public int getMoney() {
        return money;
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

    public Inventory getInventory() {
        return inventory;
    }
}
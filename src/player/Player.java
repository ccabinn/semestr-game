package player;

import inventory.Inventory;
import inventory.ItemCatalog;
import map.Location;
import map.TransportType;

public class Player {

    private Location currentLocation;
    private int energy = 100;
    private int money = 20;
    private TransportType bestVehicle = TransportType.BIKE;
    private boolean hasEngineTuning = false;
    private final Inventory inventory;

    public Player(Location startLocation, ItemCatalog itemCatalog) {
        this.currentLocation = startLocation;
        this.inventory = new Inventory(itemCatalog);
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

    // --- SPRÁVA PENĚZ ---
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
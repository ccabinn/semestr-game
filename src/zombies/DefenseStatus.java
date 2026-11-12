package zombies;

import inventory.Item;
import inventory.ItemCatalog;

import java.util.Map;

// Obrana farmy. Pocty (psi, ploty, ...) se berou z inventare hrace,
// sila jednoho kusu se bere z "value" v items.json - ne natvrdo z kodu.
public class DefenseStatus {
    public static final int DOG_FOOD_DAYS = 5; // jedno krmeni vydrzi psum 5 dni

    private final ItemCatalog catalog;

    private int guardDogs;
    private int dogFoodDaysLeft; // jediny udaj, ktery se NEbere z inventare, pamatujeme si ho tady
    private int woodenFences;
    private int electricFences;
    private int turrets;
    private boolean hasRifle;
    private boolean hasShotgun;

    public DefenseStatus(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    // prepocita pocty obrannych prvku podle aktualniho inventare
    public void updateFromInventory(Map<String, Integer> items) {
        guardDogs = items.getOrDefault("pes", 0);
        woodenFences = items.getOrDefault("plot", 0);
        electricFences = items.getOrDefault("elektricka_ohrada", 0);
        turrets = items.getOrDefault("turret", 0);
        hasRifle = items.getOrDefault("puska", 0) > 0;
        hasShotgun = items.getOrDefault("brokovnice", 0) > 0;
    }

    // sila jednoho kusu predmetu podle items.json
    private int valueOf(String itemId) {
        Item item = catalog.getItem(itemId);
        return item != null ? item.getValue() : 0;
    }

    // ===== utok na zombiky (zabiji je) =====

    // psi utoci jen kdyz jsou nakrmeni
    public int getEffectiveDogDamage() {
        return areDogsFed() ? guardDogs * valueOf("pes") : 0;
    }

    // zaplava turrety zkratuje
    public int getTurretDamage(boolean isFlooded) {
        return isFlooded ? 0 : turrets * valueOf("turret");
    }

    public int getPlayerWeaponDamage() {
        int damage = 0;
        if (hasRifle) damage += valueOf("puska");
        if (hasShotgun) damage += valueOf("brokovnice");
        return damage;
    }

    // ===== ploty (pohlti utok prezivsich zombiku) =====

    // zaplava zkratuje elektrickou ohradu, dreveny plot vydrzi
    public int getFenceAbsorption(boolean isFlooded) {
        int electric = isFlooded ? 0 : electricFences * valueOf("elektricka_ohrada");
        return woodenFences * valueOf("plot") + electric;
    }

    // ===== krmeni psu =====

    public void feedDogs() {
        dogFoodDaysLeft = DOG_FOOD_DAYS; // podle GDD se odpocet nastavi na 5, nescita se
    }

    // pouze pro nacteni ulozene hry (0 az 5 dni)
    public void restoreDogFoodDays(int days) {
        dogFoodDaysLeft = Math.max(0, Math.min(DOG_FOOD_DAYS, days));
    }

    public boolean areDogsFed() {
        return dogFoodDaysLeft > 0;
    }

    // vola GameManager kazdou noc (i kdyz zombici neprijdou)
    public void advanceDay() {
        if (dogFoodDaysLeft > 0) {
            dogFoodDaysLeft--;
        }
    }

    // ===== gettery =====
    public int getGuardDogs() { return guardDogs; }
    public int getDogFoodDaysLeft() { return dogFoodDaysLeft; }
    public int getWoodenFences() { return woodenFences; }
    public int getElectricFences() { return electricFences; }
    public int getTurrets() { return turrets; }
    public boolean hasRifle() { return hasRifle; }
    public boolean hasShotgun() { return hasShotgun; }
}

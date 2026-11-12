package zombies;

import inventory.ItemCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Obrana farmy: sila z items.json, krmeni psu, vliv zaplav.
class DefenseStatusTest {

    private ItemCatalog catalog;
    private DefenseStatus defense;

    @BeforeEach
    void setUp() {
        catalog = new ItemCatalog();
        defense = new DefenseStatus(catalog);
    }

    @Test
    void valuesComeFromItemCatalog() {
        defense.updateFromInventory(Map.of("plot", 2, "turret", 1, "puska", 1));
        assertEquals(2 * catalog.getItem("plot").getValue(), defense.getFenceAbsorption(false));
        assertEquals(catalog.getItem("turret").getValue(), defense.getTurretDamage(false));
        assertEquals(catalog.getItem("puska").getValue(), defense.getPlayerWeaponDamage());
    }

    @Test
    void hungryDogsDoNotDefend() {
        defense.updateFromInventory(Map.of("pes", 2));
        assertEquals(0, defense.getEffectiveDogDamage());
        defense.feedDogs();
        assertEquals(2 * catalog.getItem("pes").getValue(), defense.getEffectiveDogDamage());
    }

    @Test
    void feedingResetsToFiveDaysAndDoesNotStack() {
        defense.feedDogs();
        defense.feedDogs();
        assertEquals(DefenseStatus.DOG_FOOD_DAYS, defense.getDogFoodDaysLeft());
    }

    @Test
    void dogFoodRunsOutAfterFiveDays() {
        defense.feedDogs();
        for (int i = 0; i < 5; i++) defense.advanceDay();
        assertFalse(defense.areDogsFed());
        defense.advanceDay(); // nejde pod nulu
        assertEquals(0, defense.getDogFoodDaysLeft());
    }

    @Test
    void floodDisablesElectricDefenseButNotWoodenFence() {
        defense.updateFromInventory(Map.of("plot", 1, "elektricka_ohrada", 1, "turret", 1));
        assertEquals(catalog.getItem("plot").getValue(), defense.getFenceAbsorption(true));
        assertEquals(0, defense.getTurretDamage(true));
    }

    @Test
    void inventoryUpdateKeepsDogFood() {
        defense.feedDogs();
        defense.updateFromInventory(Map.of("pes", 1));
        assertEquals(DefenseStatus.DOG_FOOD_DAYS, defense.getDogFoodDaysLeft());
    }
}

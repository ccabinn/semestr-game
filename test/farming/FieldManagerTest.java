package farming;

import inventory.ItemCatalog;
import map.GamePlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import player.Player;

import static org.junit.jupiter.api.Assertions.*;

// Pole: sazeni, odemceni ovoce, zalevani za energii, kropitka.
class FieldManagerTest {

    private ItemCatalog itemCatalog;
    private FieldManager field;
    private Player player;

    @BeforeEach
    void setUp() {
        itemCatalog = new ItemCatalog();
        field = new FieldManager(new CropCatalog());
        player = new Player(new GamePlan().getLocationById("field"), itemCatalog);
    }

    @Test
    void plantingConsumesSeed() {
        player.getInventory().addItem("seminko_obili", 2);
        field.plantCrop("obili", player, itemCatalog);
        assertEquals(1, player.getInventory().getItemCount("seminko_obili"));
        assertFalse(field.getPlots().get(0).isEmpty());
    }

    @Test
    void cannotPlantWithoutSeed() {
        field.plantCrop("obili", player, itemCatalog);
        assertTrue(field.getPlots().stream().allMatch(Plot::isEmpty));
    }

    @Test
    void onlySixPlotsCanBeUsed() {
        player.getInventory().addItem("seminko_obili", 7);
        for (int i = 0; i < 7; i++) {
            field.plantCrop("obili", player, itemCatalog);
        }
        assertEquals(1, player.getInventory().getItemCount("seminko_obili")); // sedme se nevesel
    }

    @Test
    void fruitIsLockedUntilAllEightVegetablesPlanted() {
        player.getInventory().addItem("seminko_jahody", 1);
        field.plantCrop("jahody", player, itemCatalog);
        assertEquals(1, player.getInventory().getItemCount("seminko_jahody")); // nezasadilo se

        // zasadime vsech 8 druhu zeleniny (pole ma jen 6 zahonku, tak mezitim uklidime)
        String[] vegetables = {"obili", "salat", "cibule", "spenat", "mrkev", "brambory", "dyne", "cesnek"};
        for (String veg : vegetables) {
            player.getInventory().addItem("seminko_" + veg, 1);
            field.plantCrop(veg, player, itemCatalog);
            field.getPlots().forEach(Plot::clear);
        }
        assertTrue(player.canPlantFruit());

        field.plantCrop("jahody", player, itemCatalog);
        assertEquals(0, player.getInventory().getItemCount("seminko_jahody"));
    }

    @Test
    void wateringCostsFiveEnergyPerPlot() {
        player.getInventory().addItem("seminko_obili", 2);
        field.plantCrop("obili", player, itemCatalog);
        field.plantCrop("obili", player, itemCatalog);
        field.tendPlots(player, false, false);
        assertEquals(90, player.getEnergy());
    }

    @Test
    void droughtDoublesWateringCost() {
        player.getInventory().addItem("seminko_obili", 1);
        field.plantCrop("obili", player, itemCatalog);
        field.tendPlots(player, true, false);
        assertEquals(90, player.getEnergy());
    }

    @Test
    void wateringDuringFloodCostsNoEnergy() {
        player.getInventory().addItem("seminko_obili", 1);
        field.plantCrop("obili", player, itemCatalog);
        field.tendPlots(player, false, true);
        assertEquals(100, player.getEnergy());
    }

    @Test
    void tendingRemovesWitheredPlants() {
        player.getInventory().addItem("seminko_obili", 1);
        field.plantCrop("obili", player, itemCatalog);
        field.advanceDay(false, false);                 // nezalito -> uschne
        assertTrue(field.getPlots().get(0).isWithered());
        field.tendPlots(player, false, false);
        assertTrue(field.getPlots().get(0).isEmpty());
    }

    @Test
    void sprinklersLowerCostButMaxFour() {
        for (int i = 0; i < 5; i++) field.addSprinkler();
        assertEquals(4, field.getSprinklerCount());

        player.getInventory().addItem("seminko_obili", 1);
        field.plantCrop("obili", player, itemCatalog);
        field.tendPlots(player, false, false);
        assertEquals(99, player.getEnergy()); // 5 - 4 kropitka = 1 energie
    }
}

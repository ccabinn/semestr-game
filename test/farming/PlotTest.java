package farming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Zahonek: rust, zalevani obden / kazdy den, usychani, sklizen.
class PlotTest {

    // vlastni plodiny pro testy (nezavisle na crops.json)
    private final Crop vegetable = new Crop("test_veg", "seminko_obili", "obili",
            CropCategory.VEGETABLE, 2, WateringFrequency.EVERY_OTHER_DAY);
    private final Crop fruit = new Crop("test_fruit", "seminko_jahody", "jahody",
            CropCategory.FRUIT, 2, WateringFrequency.EVERY_DAY);

    private Plot plot;

    @BeforeEach
    void setUp() {
        plot = new Plot(1);
    }

    @Test
    void cannotPlantIntoOccupiedPlot() {
        assertTrue(plot.plant(vegetable));
        assertFalse(plot.plant(fruit));
        assertSame(vegetable, plot.getCrop());
    }

    @Test
    void wateredVegetableGrowsAndIsHarvestable() {
        plot.plant(vegetable);
        plot.water();
        plot.advanceDay(false);       // den 1: zalito -> roste
        plot.advanceDay(false);       // den 2: obden, vcera zalito -> nemusi se zalevat
        assertTrue(plot.isMature());
        assertSame(vegetable, plot.harvest());
        assertTrue(plot.isEmpty());   // po sklizni je zahonek volny
    }

    @Test
    void unwateredPlantWithers() {
        plot.plant(vegetable);
        plot.advanceDay(false); // hned prvni den nezalito
        assertTrue(plot.isWithered());
        assertNull(plot.harvest());
    }

    @Test
    void fruitNeedsWaterEveryDay() {
        plot.plant(fruit);
        plot.water();
        plot.advanceDay(false);
        assertFalse(plot.isWithered());
        plot.advanceDay(false); // druhy den nezalito -> ovoce uschne
        assertTrue(plot.isWithered());
    }

    @Test
    void droughtRequiresWateringEveryDayEvenForVegetables() {
        plot.plant(vegetable);
        plot.water();
        plot.advanceDay(true);
        assertTrue(plot.requiresWateringToday(true));
        plot.advanceDay(true); // obden by stacilo, ale je sucho
        assertTrue(plot.isWithered());
    }

    @Test
    void immaturePlantCannotBeHarvested() {
        plot.plant(vegetable);
        plot.water();
        plot.advanceDay(false);
        assertFalse(plot.isMature());
        assertEquals(1, plot.getDaysUntilHarvest());
        assertNull(plot.harvest());
    }
}

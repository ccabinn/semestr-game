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
        plot.advanceDay(false, false);       // den 1: zalito -> roste
        plot.advanceDay(false, false);       // den 2: obden, vcera zalito -> nemusi se zalevat
        assertTrue(plot.isMature());
        assertSame(vegetable, plot.harvest());
        assertTrue(plot.isEmpty());   // po sklizni je zahonek volny
    }

    @Test
    void unwateredPlantWithers() {
        plot.plant(vegetable);
        plot.advanceDay(false, false); // hned prvni den nezalito
        assertTrue(plot.isWithered());
        assertNull(plot.harvest());
    }

    @Test
    void fruitNeedsWaterEveryDay() {
        plot.plant(fruit);
        plot.water();
        plot.advanceDay(false, false);
        assertFalse(plot.isWithered());
        plot.advanceDay(false, false); // druhy den nezalito -> ovoce uschne
        assertTrue(plot.isWithered());
    }

    @Test
    void droughtRequiresWateringEveryDayEvenForVegetables() {
        plot.plant(vegetable);
        plot.water();
        plot.advanceDay(true, false);
        assertTrue(plot.requiresWateringToday(true, false));
        plot.advanceDay(true, false); // obden by stacilo, ale je sucho
        assertTrue(plot.isWithered());
    }

    @Test
    void floodWatersPlantAutomatically() {
        plot.plant(fruit);              // ovoce potrebuje vodu kazdy den
        plot.advanceDay(false, true);   // nezalito, ale je zaplava
        assertFalse(plot.isWithered());
        assertFalse(plot.requiresWateringToday(false, true));
    }

    @Test
    void immaturePlantCannotBeHarvested() {
        plot.plant(vegetable);
        plot.water();
        plot.advanceDay(false, false);
        assertFalse(plot.isMature());
        assertEquals(1, plot.getDaysUntilHarvest());
        assertNull(plot.harvest());
    }
}

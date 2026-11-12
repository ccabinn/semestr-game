package inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Ukazkovy unit test - overuje zakladni logiku inventare.
// Spusteni: "mvn test" nebo zelena sipka u tridy v IntelliJ.
class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        // pred kazdym testem novy prazdny inventar (katalog se nacte z data/items.json)
        inventory = new Inventory(new ItemCatalog());
    }

    @Test
    void addedItemIsCounted() {
        inventory.addItem("chleb", 2);
        assertEquals(2, inventory.getItemCount("chleb"));
    }

    @Test
    void unknownItemIsNotAdded() {
        assertFalse(inventory.addItem("neexistujici_vec", 1));
        assertEquals(0, inventory.getItemCount("neexistujici_vec"));
    }

    @Test
    void cannotRemoveMoreThanOwned() {
        inventory.addItem("chleb", 2);
        assertFalse(inventory.removeItem("chleb", 3));
        assertEquals(2, inventory.getItemCount("chleb")); // nic se neodebralo
    }

    @Test
    void removingAllDeletesItem() {
        inventory.addItem("chleb", 2);
        assertTrue(inventory.removeItem("chleb", 2));
        assertFalse(inventory.getItems().containsKey("chleb"));
    }
}

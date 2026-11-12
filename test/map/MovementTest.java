package map;

import inventory.ItemCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import player.Player;

import static org.junit.jupiter.api.Assertions.*;

// Cestovani mezi lokalitami: dopravni prostredky a spotreba energie.
class MovementTest {

    private GamePlan gamePlan;
    private Player player;

    @BeforeEach
    void setUp() {
        gamePlan = new GamePlan(); // nacte data/locations.json
        player = new Player(gamePlan.getLocationById("home"), new ItemCatalog());
    }

    @Test
    void walkingToFieldIsFree() {
        gamePlan.movePlayer(player, "field");
        assertEquals("field", player.getCurrentLocation().getId());
        assertEquals(100, player.getEnergy());
    }

    @Test
    void bikeToMarketCosts40Energy() {
        gamePlan.movePlayer(player, "local_market");
        assertEquals("local_market", player.getCurrentLocation().getId());
        assertEquals(60, player.getEnergy());   // bylo 80
    }

    @Test
    void cityIsUnreachableByBike() {
        gamePlan.movePlayer(player, "city");
        assertEquals("home", player.getCurrentLocation().getId()); // nikam se nepohnul
        assertEquals(100, player.getEnergy());
    }

    @Test
    void carReachesCityAndIsCheaperToMarket() {
        player.setBestVehicle(TransportType.CAR);
        gamePlan.movePlayer(player, "city");
        assertEquals("city", player.getCurrentLocation().getId());
        assertEquals(60, player.getEnergy());   // auto do Mesta = 40 (cesta tam i zpet)
    }

    @Test
    void notEnoughEnergyBlocksTravel() {
        player.setEnergy(10);
        gamePlan.movePlayer(player, "local_market"); // na kole stoji 40
        assertEquals("home", player.getCurrentLocation().getId());
        assertEquals(10, player.getEnergy());
    }

    @Test
    void engineTuningLowersCarCostByFive() {
        Passage toCity = gamePlan.getLocationById("home").getPassage("city");
        assertEquals(40, toCity.getEnergyCost(TransportType.CAR, false));
        assertEquals(35, toCity.getEnergyCost(TransportType.CAR, true));
        assertEquals(15, toCity.getEnergyCost(TransportType.VAN, true));
    }
}

package inventory;

import assets.NpcManager;
import loader.NpcLoader;
import map.GamePlan;
import map.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import player.Player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Obchod: nakup/prodej, ceny na trzich, zamky a slevy z ukolu, vozidla.
class ShopManagerTest {

    private ItemCatalog catalog;
    private NpcManager npcManager;
    private ShopManager shop;
    private GamePlan gamePlan;
    private Player player;

    @BeforeEach
    void setUp() {
        catalog = new ItemCatalog();
        npcManager = NpcLoader.loadNpcsFromJSON("/data/npcs.json", catalog);
        shop = new ShopManager(catalog, npcManager);
        gamePlan = new GamePlan();
        player = new Player(gamePlan.getLocationById("local_market"), catalog);
        player.addMoney(1000); // 20 startovnich + 1000
    }

    private void goTo(String locationId) {
        player.setCurrentLocation(gamePlan.getLocationById(locationId));
    }

    @Test
    void buyingTakesMoneyAndAddsItem() {
        shop.buyItem("chleb", 3, player);
        assertEquals(3, player.getInventory().getItemCount("chleb"));
        assertEquals(1020 - 3 * 8, player.getMoney());
    }

    @Test
    void cannotBuyWithoutEnoughMoney() {
        Player poor = new Player(gamePlan.getLocationById("local_market"), catalog); // ma jen 20 Kc
        shop.buyItem("pes", 1, poor);
        assertEquals(0, poor.getInventory().getItemCount("pes"));
        assertEquals(20, poor.getMoney());
    }

    @Test
    void cannotBuyItemNotSoldInThisLocation() {
        shop.buyItem("turret", 1, player); // turret je jen v Army shopu
        assertEquals(0, player.getInventory().getItemCount("turret"));
        assertEquals(1020, player.getMoney());
    }

    @Test
    void cropSellsForDoubleOnCityMarket() {
        player.getInventory().addItem("obili", 2);
        int before = player.getMoney();
        shop.sellItem("obili", 1, player);
        int localPrice = player.getMoney() - before;

        goTo("city_market");
        before = player.getMoney();
        shop.sellItem("obili", 1, player);
        assertEquals(localPrice * 2, player.getMoney() - before);
    }

    @Test
    void cannotSellOutsideMarkets() {
        goTo("army_shop");
        player.getInventory().addItem("obili", 1);
        shop.sellItem("obili", 1, player);
        assertEquals(1, player.getInventory().getItemCount("obili"));
    }

    @Test
    void armyTechIsLockedUntilSergeantQuest() {
        goTo("army_shop");
        shop.buyItem("turret", 1, player);
        assertEquals(0, player.getInventory().getItemCount("turret"));

        npcManager.loadStates(List.of("kral:0=DONE")); // ukol u Krale splnen
        shop.buyItem("turret", 1, player);
        assertEquals(1, player.getInventory().getItemCount("turret"));
    }

    @Test
    void weaponDiscountAfterSergeantQuest() {
        Item rifle = catalog.getItem("puska");
        assertEquals(120, shop.getPrice(rifle));
        npcManager.loadStates(List.of("kral:0=DONE"));
        assertEquals(96, shop.getPrice(rifle)); // -20 %
    }

    @Test
    void vanDiscountAfterRomanQuest() {
        Item van = catalog.getItem("dodavka");
        assertEquals(300, shop.getPrice(van));
        npcManager.loadStates(List.of("roman:0=DONE"));
        assertEquals(220, shop.getPrice(van));
    }

    @Test
    void buyingVehicleUpgradesTransportNotInventory() {
        shop.buyItem("auto", 1, player);
        assertEquals(TransportType.CAR, player.getBestVehicle());
        assertEquals(0, player.getInventory().getItemCount("auto"));
        assertEquals(1020 - 150, player.getMoney());
    }

    @Test
    void cannotBuyWorseOrSameVehicleAgain() {
        player.setBestVehicle(TransportType.VAN);
        shop.buyItem("auto", 1, player);
        assertEquals(TransportType.VAN, player.getBestVehicle());
        assertEquals(1020, player.getMoney()); // penize se nestrhly
    }
}

package game;

import map.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.JsonGameStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Test ulozeni a nacteni cele hry pres prikazy "uloz" a "nacti".
class SaveLoadTest {

    @TempDir
    Path tempDir;

    private JsonGameStorage storage;
    private GameManager game;

    @BeforeEach
    void setUp() {
        storage = new JsonGameStorage(tempDir);
        game = new GameManager(storage);
    }

    @Test
    void savedGameIsRestoredInNewSession() {
        // rozehrajeme hru: zasadime, koupime auto, rozjedeme ukol
        game.processCommand("jdi field");
        game.processCommand("zasad obili");
        game.processCommand("obstarej");
        game.getPlayer().addMoney(500);
        game.processCommand("jdi home");
        game.processCommand("jdi local_market");
        game.processCommand("kup auto");
        game.processCommand("mluv bohous");
        int energy = game.getPlayer().getEnergy();
        int money = game.getPlayer().getMoney();

        assertTrue(game.processCommand("uloz test").startsWith("Hra uložena"));

        // "vypneme hru" a nacteme ulozeni do uplne nove instance
        GameManager loaded = new GameManager(storage);
        assertTrue(loaded.processCommand("nacti test").startsWith("Načtena hra"));

        assertEquals("local_market", loaded.getPlayer().getCurrentLocation().getId());
        assertEquals(energy, loaded.getPlayer().getEnergy());
        assertEquals(money, loaded.getPlayer().getMoney());
        assertEquals(TransportType.CAR, loaded.getPlayer().getBestVehicle());
        assertEquals(1, loaded.getPlayer().getInventory().getItemCount("seminko_obili"));
        assertEquals("wheat", loaded.getFieldManager().getPlots().get(0).getCrop().getId());
        assertTrue(loaded.getFieldManager().getPlots().get(0).isWateredToday());
        assertEquals(1, loaded.getPlayer().getPlantedVegetablesCount());
        // ukol u Bohouse zustal rozjety -> "ukol" ho nechce znovu zahajovat
        assertFalse(loaded.processCommand("ukol bohous").contains("Nejdřív si s postavou promluv"));
    }

    @Test
    void loadingMissingSaveKeepsCurrentGame() {
        game.getPlayer().addMoney(100);
        String result = game.processCommand("nacti neexistuje");
        assertTrue(result.startsWith("Hru se nepodařilo načíst"));
        assertEquals(120, game.getPlayer().getMoney()); // nic se neprepsalo
    }

    @Test
    void invalidSaveFileIsRejectedWithoutChanges() throws IOException {
        game.processCommand("uloz dobra");
        // nekdo rucne prepsal lokalitu na nesmysl
        Path file = tempDir.resolve("dobra.json");
        Files.writeString(file, Files.readString(file).replace("\"home\"", "\"mesic\""));

        String result = game.processCommand("nacti dobra");
        assertTrue(result.contains("neznámá lokalita"));
        assertEquals("home", game.getPlayer().getCurrentLocation().getId());
    }

    @Test
    void corruptedQuestStateIsRejectedWithoutCrash() throws IOException {
        game.processCommand("uloz ukoly");
        Path file = tempDir.resolve("ukoly.json");
        Files.writeString(file, Files.readString(file).replaceFirst("=INACTIVE", "=HOTOVO"));

        String result = game.processCommand("nacti ukoly"); // drive vyhodilo vyjimku
        assertTrue(result.contains("poškozený stav úkolů"));
    }

    @Test
    void savesAreListed() {
        game.processCommand("uloz prvni");
        game.processCommand("uloz druha");
        assertEquals("Uložené hry: druha, prvni", game.processCommand("ulozene"));
    }
}

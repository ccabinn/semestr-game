package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.JsonGameStorage;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Prohra: zombici zniceni urody a bankrot (zadne penize, veci ani uroda).
class GameOverTest {

    @TempDir
    Path tempDir;

    private GameManager game;

    @BeforeEach
    void setUp() {
        game = new GameManager(new JsonGameStorage(tempDir));
    }

    // hrac nema nic: zadne penize, prazdny inventar, prazdne pole
    private void makeBroke() {
        game.getPlayer().removeMoney(game.getPlayer().getMoney());
        game.getPlayer().getInventory().clear();
    }

    @Test
    void brokenDefenseDestroysAllCrops() {
        game.processCommand("setday 12");             // od 10. dne utoci zombici
        game.processCommand("jdi field");
        game.processCommand("zasad obili");
        game.processCommand("obstarej");
        game.processCommand("jdi home");

        String night = game.processCommand("spi");    // zadna obrana -> prolomeni
        assertTrue(night.contains("ZNIČILI veškerou úrodu"));
        assertTrue(game.getFieldManager().getPlots().stream().allMatch(p -> p.isEmpty()));
    }

    @Test
    void bankruptPlayerLoses() {
        makeBroke();
        String night = game.processCommand("spi");
        assertTrue(night.contains("GAME OVER"));
        assertTrue(game.isGameOver());
    }

    @Test
    void playerWithMoneyForSeedIsNotBankrupt() {
        makeBroke();
        game.getPlayer().addMoney(1); // nejlevnejsi semínko (obili) stoji 1 Kc
        game.processCommand("spi");
        assertFalse(game.isGameOver());
    }

    @Test
    void afterGameOverOnlyLoadingWorks() {
        game.processCommand("uloz zaloha");
        makeBroke();
        game.processCommand("spi");
        assertTrue(game.isGameOver());

        assertTrue(game.processCommand("jdi field").startsWith("Hra skončila"));
        assertTrue(game.processCommand("nacti zaloha").startsWith("Načtena hra"));
        assertFalse(game.isGameOver());           // po nacteni se hraje dal
        assertEquals(20, game.getPlayer().getMoney());
    }
}

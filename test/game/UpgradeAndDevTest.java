package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.JsonGameStorage;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Kropitka ziskana nakupem i odmenou a vyvojarsky prikaz growall.
class UpgradeAndDevTest {

    @TempDir
    Path tempDir;

    private GameManager game;

    @BeforeEach
    void setUp() {
        game = new GameManager(new JsonGameStorage(tempDir));
    }

    @Test
    void boughtSprinklerWorksOnField() {
        game.getPlayer().addMoney(100);
        game.processCommand("tp city_market");
        game.processCommand("kup kropitko");
        assertEquals(1, game.getFieldManager().getSprinklerCount());
    }

    @Test
    void sprinklerFromBohousWorksOnField() {
        game.getPlayer().getInventory().addItem("kolac", 1);
        game.processCommand("jdi local_market");
        game.processCommand("mluv bohous");
        game.processCommand("ukol bohous");
        assertEquals(1, game.getFieldManager().getSprinklerCount()); // drive 0
    }

    @Test
    void growAllDoesNotHangOnUnwateredPlant() {
        game.processCommand("jdi field");
        game.processCommand("zasad obili");                           // nezalito
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),
                () -> game.processCommand("growall"));                 // drive nekonecna smycka
        assertTrue(game.getFieldManager().getPlots().get(0).isMature());
    }

    @Test
    void devMoneyAcceptsNegativeAmount() {
        game.processCommand("money -5");
        assertEquals(15, game.getPlayer().getMoney());
    }
}

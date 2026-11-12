package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.JsonGameStorage;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Obrana koupena behem dne se musi v noci pocitat (i kdyz hrac nezavola "stav").
class NightDefenseTest {

    @TempDir
    Path tempDir;

    private GameManager game;

    @BeforeEach
    void setUp() {
        game = new GameManager(new JsonGameStorage(tempDir));
        game.processCommand("setday 10"); // od 10. dne utoci zombici
    }

    @Test
    void newFencesDefendTheSameNight() {
        game.getPlayer().getInventory().addItem("plot", 3); // jako by je prave koupil
        String night = game.processCommand("spi");          // bez "stav" predtim
        assertFalse(night.contains("pohltily 0 bodů"));
        assertTrue(night.contains("Doplněno +50 energie"));  // obrana vydrzela
    }

    @Test
    void withoutDefenseTheRaidBreaksThrough() {
        String night = game.processCommand("spi");
        assertTrue(night.contains("NEDOPLNIL si energii"));
    }
}

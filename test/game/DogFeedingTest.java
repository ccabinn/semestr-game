package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import save.JsonGameStorage;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Prikaz nakrm_psy pres celou hru (GameManager).
class DogFeedingTest {

    @TempDir
    Path tempDir;

    private GameManager game;

    @BeforeEach
    void setUp() {
        game = new GameManager(new JsonGameStorage(tempDir));
        game.getPlayer().getInventory().addItem("pes", 2);
        game.getPlayer().getInventory().addItem("krmeni_psy", 2);
        game.processCommand("jdi field");
    }

    @Test
    void feedingConsumesOneFood() {
        assertTrue(game.processCommand("nakrm_psy").startsWith("Nakrmil jsi psy"));
        assertEquals(1, game.getPlayer().getInventory().getItemCount("krmeni_psy"));
    }

    @Test
    void feedingAlreadyFullDogsDoesNotWasteFood() {
        game.processCommand("nakrm_psy");
        String second = game.processCommand("nakrm_psy");
        assertTrue(second.startsWith("Psi jsou ještě sytí"));
        assertEquals(1, game.getPlayer().getInventory().getItemCount("krmeni_psy")); // druhe se nespotrebovalo
    }

    @Test
    void dogsCanBeFedAgainAfterADay() {
        game.processCommand("nakrm_psy");
        game.processCommand("jdi home");
        game.processCommand("spi");
        game.processCommand("jdi field");
        assertTrue(game.processCommand("nakrm_psy").startsWith("Nakrmil jsi psy"));
        assertEquals(0, game.getPlayer().getInventory().getItemCount("krmeni_psy"));
    }

    @Test
    void feedingOnlyWorksOnField() {
        game.processCommand("jdi home");
        assertTrue(game.processCommand("nakrm_psy").startsWith("Psy můžeš nakrmit pouze na poli"));
        assertEquals(2, game.getPlayer().getInventory().getItemCount("krmeni_psy"));
    }
}

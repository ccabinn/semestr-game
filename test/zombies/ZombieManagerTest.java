package zombies;

import inventory.ItemCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Nocni najezd: obrana musi zombiky opravdu zabijet, jinak nema vliv.
class ZombieManagerTest {

    private ZombieManager zombieManager;
    private DefenseStatus defense;

    @BeforeEach
    void setUp() {
        zombieManager = new ZombieManager();
        defense = new DefenseStatus(new ItemCatalog());
    }

    @Test
    void noAttackBeforeDayTen() {
        NightRaidResult result = zombieManager.resolveNightRaid(9, defense, false);
        assertFalse(result.isAttackOccurred());
    }

    @Test
    void fedDogsKillZombieThatHungryDogsDoNot() {
        defense.updateFromInventory(Map.of("pes", 2));

        // hladovi psi: na 12. den nikoho nezabiji
        NightRaidResult hungry = zombieManager.resolveNightRaid(12, defense, false);
        assertEquals(0, hungry.getDefeatedZombiesCount());

        // nakrmeni psi: aspon jednoho zombika zabiji a v logu je to videt
        defense.feedDogs();
        NightRaidResult fed = zombieManager.resolveNightRaid(12, defense, false);
        assertTrue(fed.getDefeatedZombiesCount() >= 1);
        assertTrue(fed.getSummaryLog().contains("Hlídací psi"));
    }

    @Test
    void strongDefenseStopsTheRaid() {
        defense.updateFromInventory(Map.of("turret", 2, "plot", 2));
        NightRaidResult result = zombieManager.resolveNightRaid(12, defense, false);
        assertTrue(result.isDefenseSuccessful());
        assertEquals(0, result.getBreachDamage());
    }
}

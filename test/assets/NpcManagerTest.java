package assets;

import inventory.ItemCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// Ukoly NPC: aktivace rozhovorem, kontrola pozadavku, odebrani veci, odmeny.
// Misto cele hry pouzivame jednoduchou testovaci implementaci rozhrani GameContext.
class NpcManagerTest {

    // "falesna hra" - jen inventar, penize a zaznam vypisu a efektu
    private static class FakeContext implements GameContext {
        String location = "local_market";
        int money = 0;
        final Map<String, Integer> items = new HashMap<>();
        final List<String> printed = new ArrayList<>();
        final List<QuestEffect> effects = new ArrayList<>();

        @Override public String getCurrentLocationId() { return location; }
        @Override public void print(String text) { printed.add(text); }
        @Override public int countItem(String itemId) { return items.getOrDefault(itemId, 0); }
        @Override public void removeItem(String itemId, int count) { items.merge(itemId, -count, Integer::sum); }
        @Override public void addItem(String itemId, int count) { items.merge(itemId, count, Integer::sum); }
        @Override public void addMoney(int amount) { money += amount; }
        @Override public void removeMoney(int amount) { money -= amount; }
        @Override public int getMoney() { return money; }
        @Override public void onQuestEffect(QuestEffect effect) { effects.add(effect); }
    }

    private NpcManager manager;
    private FakeContext ctx;
    private Quest quest;

    @BeforeEach
    void setUp() {
        manager = new NpcManager(new ItemCatalog());
        ctx = new FakeContext();

        // testovaci ukol: 2x mrkev + 50 Kc -> odmena 100 Kc, kropitko a efekt
        quest = new Quest("Test",
                List.of(new Requirement(List.of("mrkev"), 2), new Requirement(List.of(Requirement.MONEY), 50)),
                100, Map.of("kropitko", 1), Set.of(QuestEffect.TUNING_MOTORU),
                "intro", "jeste ne", "diky");
        manager.addNpc("tester", new Npc("Tester", "local_market", "ahoj", new ArrayList<>(List.of(quest))));
    }

    @Test
    void talkingActivatesQuest() {
        assertEquals(QuestState.INACTIVE, quest.getQuestState());
        manager.talk("tester", ctx);
        assertEquals(QuestState.ACTIVE, quest.getQuestState());
    }

    @Test
    void cannotHandInBeforeTalking() {
        ctx.items.put("mrkev", 2);
        ctx.money = 50;
        manager.handIn("tester", ctx);
        assertEquals(QuestState.INACTIVE, quest.getQuestState());
        assertEquals(2, ctx.countItem("mrkev"));
    }

    @Test
    void missingRequirementTakesNothing() {
        manager.talk("tester", ctx);
        ctx.items.put("mrkev", 2);
        ctx.money = 10; // chybi penize
        manager.handIn("tester", ctx);
        assertEquals(QuestState.ACTIVE, quest.getQuestState());
        assertEquals(2, ctx.countItem("mrkev")); // mrkev se NEodebrala, i kdyz ji ma
        assertEquals(10, ctx.money);
    }

    @Test
    void completedQuestTakesItemsAndGivesRewards() {
        manager.talk("tester", ctx);
        ctx.items.put("mrkev", 3);
        ctx.money = 60;
        manager.handIn("tester", ctx);

        assertEquals(QuestState.DONE, quest.getQuestState());
        assertEquals(1, ctx.countItem("mrkev"));          // 3 - 2
        assertEquals(60 - 50 + 100, ctx.money);           // zaplatil 50, dostal 100
        assertEquals(1, ctx.countItem("kropitko"));
        assertEquals(List.of(QuestEffect.TUNING_MOTORU), ctx.effects);
        assertTrue(manager.hasEffect(QuestEffect.TUNING_MOTORU));
    }

    @Test
    void npcInOtherLocationIsNotAvailable() {
        ctx.location = "city";
        manager.talk("tester", ctx);
        assertEquals(QuestState.INACTIVE, quest.getQuestState());
    }

    @Test
    void orRequirementAcceptsAnyOption() {
        Requirement soupOrCake = new Requirement(List.of("polevka", "kolac"), 1);
        ctx.items.put("kolac", 1);
        assertTrue(soupOrCake.isMetBy(ctx));
        soupOrCake.takeFrom(ctx);
        assertEquals(0, ctx.countItem("kolac"));
    }

    @Test
    void questStatesSurviveSaveAndLoad() {
        manager.talk("tester", ctx);
        List<String> saved = manager.saveStates();

        NpcManager other = new NpcManager(new ItemCatalog());
        Quest copy = new Quest("Test", List.of(), 0, Map.of(), Set.of(), "", "", "");
        other.addNpc("tester", new Npc("Tester", "local_market", "", new ArrayList<>(List.of(copy))));
        other.loadStates(saved);
        assertEquals(QuestState.ACTIVE, copy.getQuestState());
    }
}

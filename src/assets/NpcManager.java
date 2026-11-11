package assets;

import inventory.Item;
import inventory.ItemCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Drzi vsechna NPC (klic = id z npcs.json) a obsluhuje prikazy "mluv" a "ukol".
public class NpcManager {
    private final Map<String, Npc> npcs = new HashMap<>();
    private final ItemCatalog catalog; // jen kvuli nazvum predmetu ve vypisech

    public NpcManager(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    public void addNpc(String id, Npc npc) {
        npcs.put(id, npc);
    }

    public Npc getNpc(String id) {
        return npcs.get(id);
    }

    // prikaz "mluv": prvni rozhovor ukol aktivuje, dalsi jen pripomene, co chybi
    public void talk(String npcId, GameContext ctx) {
        Npc npc = findNpcHere(npcId, ctx);
        if (npc == null) return;

        Quest quest = npc.getCurrentQuest();
        if (quest == null) {
            // NPC bez ukolu nebo se vsemi ukoly splnenymi
            ctx.print(npc.getName() + ": \"" + npc.getGreeting() + "\"");
            return;
        }

        if (quest.getQuestState() == QuestState.INACTIVE) {
            quest.setQuestState(QuestState.ACTIVE);
            ctx.print(npc.getName() + ": \"" + quest.getIntroDialog() + "\"");
        } else {
            ctx.print(npc.getName() + ": \"" + quest.getNotDoneDialog() + "\"");
        }
        ctx.print("Úkol: " + quest.getName());
        ctx.print(quest.describeRequirements(catalog));
    }

    // prikaz "ukol": zkontroluje pozadavky, odebere je a preda odmenu
    public void handIn(String npcId, GameContext ctx) {
        Npc npc = findNpcHere(npcId, ctx);
        if (npc == null) return;

        Quest quest = npc.getCurrentQuest();
        if (quest == null) {
            ctx.print(npc.getName() + " pro tebe nemá žádný úkol.");
            return;
        }
        if (quest.getQuestState() == QuestState.INACTIVE) {
            ctx.print("Nejdřív si s postavou promluv (mluv " + npcId + ").");
            return;
        }

        // nejdriv zkontrolovat VSE, az pak odebirat (aby hrac neprisel o pulku veci)
        for (Requirement r : quest.getRequirements()) {
            if (!r.isMetBy(ctx)) {
                ctx.print(npc.getName() + ": \"" + quest.getNotDoneDialog() + "\"");
                ctx.print("Chybí ti: " + r.describe(catalog));
                return;
            }
        }
        for (Requirement r : quest.getRequirements()) {
            r.takeFrom(ctx);
        }

        quest.setQuestState(QuestState.DONE);
        ctx.print(npc.getName() + ": \"" + quest.getFinalDialog() + "\"");
        ctx.print("Úkol \"" + quest.getName() + "\" splněn!");

        // odmeny
        if (quest.getRewardMoney() > 0) {
            ctx.addMoney(quest.getRewardMoney());
            ctx.print("Odměna: +" + quest.getRewardMoney() + " Kč.");
        }
        for (Map.Entry<String, Integer> reward : quest.getRewardItems().entrySet()) {
            ctx.addItem(reward.getKey(), reward.getValue());
            Item item = catalog.getItem(reward.getKey());
            String name = item != null ? item.getName() : reward.getKey();
            ctx.print("Odměna: +" + reward.getValue() + "× " + name + ".");
        }
        for (QuestEffect effect : quest.getEffects()) {
            ctx.onQuestEffect(effect);
        }
    }

    // vrati NPC, jen pokud existuje a stoji v aktualni lokalite hrace
    private Npc findNpcHere(String npcId, GameContext ctx) {
        Npc npc = npcs.get(npcId);
        if (npc == null) {
            ctx.print("Neznámá postava.");
            return null;
        }
        if (!npc.getLocationId().equals(ctx.getCurrentLocationId())) {
            ctx.print(npc.getName() + " tu není.");
            return null;
        }
        return npc;
    }

    // ma hrac odemceny dany efekt? (= je hotovy ukol, ktery ho dava)
    // stav se pocita z ukolu, takze po nacteni hry (loadStates) sedi automaticky
    public boolean hasEffect(QuestEffect effect) {
        for (Npc npc : npcs.values()) {
            for (Quest q : npc.getQuests()) {
                if (q.getQuestState() == QuestState.DONE && q.getEffects().contains(effect)) {
                    return true;
                }
            }
        }
        return false;
    }

    // pro ulozeni hry: radky ve tvaru "elena:1=ACTIVE"
    public List<String> saveStates() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Npc> e : npcs.entrySet()) {
            List<Quest> quests = e.getValue().getQuests();
            for (int i = 0; i < quests.size(); i++) {
                lines.add(e.getKey() + ":" + i + "=" + quests.get(i).getQuestState());
            }
        }
        return lines;
    }

    // pro nacteni hry: opak saveStates, neplatne radky preskoci
    public void loadStates(List<String> lines) {
        for (String line : lines) {
            String[] kv = line.split("=", 2);
            String[] key = kv[0].split(":", 2);
            if (kv.length < 2 || key.length < 2) continue;
            Npc npc = npcs.get(key[0]);
            if (npc == null) continue;
            int index = Integer.parseInt(key[1]);
            if (index < 0 || index >= npc.getQuests().size()) continue;
            npc.getQuests().get(index).setQuestState(QuestState.valueOf(kv[1].trim()));
        }
    }
}

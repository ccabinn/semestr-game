package assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcManager {
    private final Map<String, Npc> npcs = new HashMap<>();
    private final ItemCatalog catalog;

    public NpcManager(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    public NpcManager() {
        this(new ItemCatalog());
    }

    public void addNpc(String id, Npc npc) {
        npcs.put(id, npc);
    }

    public Npc getNpc(String id) {
        return npcs.get(id);
    }

    public void talk(String npcId, GameContext ctx) {
        Npc npc = npcs.get(npcId);
        if (npc == null) {
            ctx.print("Neznámá postava.");
            return;
        }
        if (!npc.getLocationId().equals(ctx.getCurrentLocationId())) {
            ctx.print(npc.getName() + " tu není.");
            return;
        }

        Quest quest = npc.getCurrentQuest();
        if (quest == null) {
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

    public void handIn(String npcId, GameContext ctx) {
        Npc npc = npcs.get(npcId);
        if (npc == null) {
            ctx.print("Neznámá postava.");
            return;
        }
        if (!npc.getLocationId().equals(ctx.getCurrentLocationId())) {
            ctx.print(npc.getName() + " tu není.");
            return;
        }

        Quest quest = npc.getCurrentQuest();
        if (quest == null) {
            ctx.print(npc.getName() + " pro tebe nemá žádný úkol.");
            return;
        }
        if (quest.getQuestState() == QuestState.INACTIVE) {
            ctx.print("Nejdřív si s postavou promluv (mluv " + npcId + ").");
            return;
        }

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
        if (quest.getRewardMoney() > 0) {
            ctx.addMoney(quest.getRewardMoney());
            ctx.print("Odměna: +" + quest.getRewardMoney() + " Kč.");
        }
        for (QuestEffect effect : quest.getEffects()) {
            ctx.onQuestEffect(effect);
        }
    }

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

    public void loadStates(List<String> lines) {
        for (String line : lines) {
            String[] kv = line.split("=", 2);
            String[] key = kv[0].split(":", 2);
            Npc npc = npcs.get(key[0]);
            if (npc == null) continue;
            int index = Integer.parseInt(key[1]);
            if (index < 0 || index >= npc.getQuests().size()) continue;
            npc.getQuests().get(index).setQuestState(QuestState.valueOf(kv[1].trim()));
        }
    }
}
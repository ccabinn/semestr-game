package assets;

import java.util.HashMap;
import java.util.Map;

public class NpcManager {
    private final Map<String, Npc> npcs = new HashMap<>();

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
        ctx.print(quest.describeRequirements());
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

        //kontrola
        for (Requirement r : quest.getRequirements()) {
            if (!r.isMetBy(ctx)) {
                ctx.print(npc.getName() + ": \"" + quest.getNotDoneDialog() + "\"");
                ctx.print("Chybí ti: " + r);
                return;
            }
        }

        //odebirani
        for (Requirement r : quest.getRequirements()) {
            r.takeFrom(ctx);
        }

        //completion
        quest.setQuestState(QuestState.DONE);
        ctx.print(npc.getName() + ": \"" + quest.getFinalDialog() + "\"");
        ctx.print("Úkol \"" + quest.getName() + "\" splněn!");
        if (quest.getRewardMoney() > 0) {
            ctx.addMoney(quest.getRewardMoney());
            ctx.print("Odměna: +" + quest.getRewardMoney() + " Kč.");
        }
    }
}

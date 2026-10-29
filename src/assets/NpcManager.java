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
            ctx.print("neznámá postava.");
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
}

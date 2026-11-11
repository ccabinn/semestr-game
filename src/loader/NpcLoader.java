package loader;

import assets.Npc;
import assets.NpcManager;
import assets.Quest;
import assets.QuestEffect;
import assets.Requirement;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import inventory.ItemCatalog;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Nacte NPC a jejich ukoly z data/npcs.json (pres Gson, stejne jako ostatni loadery).
public class NpcLoader {

    // Pomocne tridy kopirujici strukturu JSONu. Gson do nich zapise data
    // a z nich pak postavime "skutecne" objekty Npc / Quest / Requirement.
    private static class NpcData {
        String id;
        String name;
        String locationId;
        String greeting;
        List<QuestData> quests;
    }

    private static class QuestData {
        String name;
        String introDialog;
        String notDoneDialog;
        String finalDialog;
        int rewardMoney;
        Map<String, Integer> rewardItems;
        List<RequirementData> requirements;
        List<QuestEffect> effects; // Gson prevede text "TUNING_MOTORU" na enum sam
    }

    private static class RequirementData {
        List<String> itemIds;
        int itemCount;
    }

    public static NpcManager loadNpcsFromJSON(String resourcePath, ItemCatalog catalog) {
        NpcManager manager = new NpcManager(catalog);

        try (InputStream is = NpcLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Soubor s postavami nebyl nalezen v data: " + resourcePath);
                return manager;
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<NpcData>>(){}.getType();
                List<NpcData> npcs = new Gson().fromJson(reader, listType);

                if (npcs != null) {
                    for (NpcData data : npcs) {
                        manager.addNpc(data.id.toLowerCase(), toNpc(data));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Chyba při načítání postav: " + e.getMessage());
        }

        return manager;
    }

    private static Npc toNpc(NpcData data) {
        List<Quest> quests = new ArrayList<>();
        if (data.quests != null) {
            for (QuestData q : data.quests) {
                quests.add(toQuest(q));
            }
        }
        return new Npc(data.name, data.locationId, data.greeting, quests);
    }

    private static Quest toQuest(QuestData q) {
        List<Requirement> requirements = new ArrayList<>();
        if (q.requirements != null) {
            for (RequirementData r : q.requirements) {
                requirements.add(new Requirement(r.itemIds, r.itemCount));
            }
        }

        // chybejici pole v JSONu = prazdna odmena (ne null)
        Map<String, Integer> rewardItems = q.rewardItems != null ? q.rewardItems : new HashMap<>();
        Set<QuestEffect> effects = new HashSet<>();
        if (q.effects != null) {
            for (QuestEffect effect : q.effects) {
                if (effect == null) {
                    // Gson vrati null, kdyz text v JSONu neodpovida zadnemu enumu (preklep)
                    System.err.println("Neznámý efekt v úkolu '" + q.name + "' - zkontroluj npcs.json");
                } else {
                    effects.add(effect);
                }
            }
        }

        return new Quest(q.name, requirements, q.rewardMoney, rewardItems, effects,
                q.introDialog, q.notDoneDialog, q.finalDialog);
    }
}

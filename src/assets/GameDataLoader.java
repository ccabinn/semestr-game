package assets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameDataLoader {

    public static void loadItems(String filePath, ItemCatalog catalog) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONArray jsonArray = new JSONArray(content);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Item item = new Item(
                obj.getString("id"),
                obj.getString("name"),
                ItemType.valueOf(obj.getString("type")),
                obj.getInt("price"),
                obj.getInt("value")
            );
            catalog.add(item);
        }
    }

    public static void loadNpcs(String filePath, NpcManager npcManager) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONArray jsonArray = new JSONArray(content);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String npcId = obj.getString("id"); // Klíč do NpcManager.npcs mapy
            
            List<Quest> quests = new ArrayList<>();
            if (obj.has("quests")) {
                JSONArray questsArr = obj.getJSONArray("quests");
                for (int q = 0; q < questsArr.length(); q++) {
                    JSONObject qObj = questsArr.getJSONObject(q);
                    
                    List<Requirement> reqs = new ArrayList<>();
                    if (qObj.has("requirements")) {
                        JSONArray reqsArr = qObj.getJSONArray("requirements");
                        for (int r = 0; r < reqsArr.length(); r++) {
                            JSONObject rObj = reqsArr.getJSONObject(r);
                            List<String> itemIds = new ArrayList<>();
                            JSONArray idsArr = rObj.getJSONArray("itemIds");
                            for (int k = 0; k < idsArr.length(); k++) {
                                itemIds.add(idsArr.getString(k));
                            }
                            reqs.add(new Requirement(itemIds, rObj.getInt("itemCount")));
                        }
                    }

                    Set<QuestEffect> effects = new HashSet<>();
                    if (qObj.has("effects")) {
                        JSONArray effArr = qObj.getJSONArray("effects");
                        for (int e = 0; e < effArr.length(); e++) {
                            effects.add(QuestEffect.valueOf(effArr.getString(e)));
                        }
                    }

                    quests.add(new Quest(
                        qObj.getString("name"),
                        reqs,
                        qObj.optInt("rewardMoney", 0),
                        qObj.getString("introDialog"),
                        qObj.getString("notDoneDialog"),
                        qObj.getString("finalDialog"),
                        effects
                    ));
                }
            }

            Npc npc = new Npc(
                obj.getString("name"),
                obj.getString("locationId"),
                obj.getString("greeting"),
                quests
            );
            npcManager.addNpc(npcId, npc);
        }
    }
}

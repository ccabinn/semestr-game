package assets;

import java.util.*;

public class Npc {
    private final String name;
    private final String locationId;
    private final String greeting;
    private final List<Quest> quests;

    public Npc(String name, String locationId, String greeting, List<Quest> quests) {
        this.name = name;
        this.greeting = greeting;
        this.locationId = locationId;
        this.quests = quests;
    }

    public String getName() {
        return name;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getGreeting() {
        return greeting;
    }

    public List<Quest> getQuests() {
        return quests;
    }

    // prvni nesplneny ukol (ukoly jdou po sobe, napr. 3 faze Eleny); null = vse splneno
    public Quest getCurrentQuest() {
        return quests.stream()
                .filter(q -> q.getQuestState() != QuestState.DONE)
                .findFirst()
                .orElse(null);
    }
}

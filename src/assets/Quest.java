package assets;

import inventory.ItemCatalog;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Quest {
    private final String name;
    private final List<Requirement> requirements;
    private QuestState questState = QuestState.INACTIVE;

    // odmeny: penize, predmety (id -> pocet) a efekty (slevy, odemceni, konec hry)
    private final int rewardMoney;
    private final Map<String, Integer> rewardItems;
    private final Set<QuestEffect> effects;

    private final String introDialog;
    private final String notDoneDialog;
    private final String finalDialog;

    public Quest(String name, List<Requirement> requirements, int rewardMoney,
                 Map<String, Integer> rewardItems, Set<QuestEffect> effects,
                 String introDialog, String notDoneDialog, String finalDialog) {
        this.name = name;
        this.requirements = requirements;
        this.rewardMoney = rewardMoney;
        this.rewardItems = rewardItems;
        this.effects = effects;
        this.introDialog = introDialog;
        this.notDoneDialog = notDoneDialog;
        this.finalDialog = finalDialog;
    }

    // vypis pozadavku pod sebe, s nazvy predmetu misto jejich ID
    public String describeRequirements(ItemCatalog catalog) {
        StringBuilder sb = new StringBuilder();
        for (Requirement r : requirements) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("- ").append(r.describe(catalog));
        }
        return sb.toString();
    }

    public String getName() { return name; }
    public List<Requirement> getRequirements() { return requirements; }
    public QuestState getQuestState() { return questState; }
    public void setQuestState(QuestState questState) { this.questState = questState; }

    public int getRewardMoney() { return rewardMoney; }
    public Map<String, Integer> getRewardItems() { return rewardItems; }
    public Set<QuestEffect> getEffects() { return effects; }

    public String getIntroDialog() { return introDialog; }
    public String getNotDoneDialog() { return notDoneDialog; }
    public String getFinalDialog() { return finalDialog; }

    @Override
    public String toString() {
        return name + " [" + questState + "]";
    }
}

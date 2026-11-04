package assets;

import java.util.List;
import java.util.Set;

public class Quest {
    private final String name;
    private final List<Requirement> requirements;
    private QuestState questState = QuestState.INACTIVE;
    private final int rewardMoney;
    private final String introDialog;
    private final String finalDialog;
    private final String notDoneDialog;
    private final Set<QuestEffect> effects;

    public String describeRequirements(ItemCatalog catalog) {
        StringBuilder sb = new StringBuilder();
        for (Requirement r : requirements) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("- ").append(r.describe(catalog));
        }
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public QuestState getQuestState() {
        return questState;
    }

    public String getIntroDialog(){
        return introDialog;
    }

    public String getFinalDialog(){
        return finalDialog;
    }

    public String getNotDoneDialog() {
        return notDoneDialog;
    }

    public int getRewardMoney() {
        return rewardMoney;
    }

    public void setQuestState(QuestState questState) {
        this.questState = questState;
    }

    public Quest(String name, List<Requirement> requirements, int rewardMoney,
                 String introDialog, String notDoneDialog, String finalDialog) {
        this(name, requirements, rewardMoney, introDialog, notDoneDialog, finalDialog, Set.of());
    }

    public Quest(String name, List<Requirement> requirements, int rewardMoney,
                 String introDialog, String notDoneDialog, String finalDialog,
                 Set<QuestEffect> effects) {
        this.name = name;
        this.requirements = requirements;
        this.rewardMoney = rewardMoney;
        this.introDialog = introDialog;
        this.notDoneDialog = notDoneDialog;
        this.finalDialog = finalDialog;
        this.effects = effects;
    }

    public Set<QuestEffect> getEffects() {
        return effects;
    }

    @Override
    public String toString() {
        return name + " [" + questState + "]";
    }
}

package assets;

import java.util.List;

public class Quest {
    private final String name;
    private final List<Requirement> requirements;
    private QuestState questState = QuestState.INACTIVE;
    private final int rewardMoney;
    private final String introDialog;
    private final String finalDialog;
    private final String notDoneDialog;

    public String describeRequirements() {
        StringBuilder sb = new StringBuilder();
        for (Requirement r : requirements) {
            sb.append("- ").append(r);
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

    public Quest(String name, List<Requirement> requirements, int rewardMoney, String introDialog, String notDoneDialog, String finalDialog) {
        this.name = name;
        this.requirements = requirements;
        this.rewardMoney = rewardMoney;
        this.introDialog = introDialog;
        this.notDoneDialog = notDoneDialog;
        this.finalDialog = finalDialog;
    }

    @Override
    public String toString() {
        return name + " [" + questState + "]";
    }
}

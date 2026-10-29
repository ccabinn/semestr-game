package assets;

import java.util.List;

public class Quest {
    private final String name;
    private final List<Requirement> requirements;
    private String dialog;
    private QuestState questState = QuestState.INACTIVE;
    private final int rewardMoney;
    private String introDialog;
    private String finalDialog;
    private String notDoneDialog;

    public String describeRequirements() {
        StringBuilder sb = new StringBuilder();
        for (Requirement r : requirements) {
            sb.append("- ").append(r).append("\n");
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

    public String getDialog() {
        return dialog;
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

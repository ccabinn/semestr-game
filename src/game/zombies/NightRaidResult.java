package game.zombies;

public class NightRaidResult {
    private final boolean attackOccurred;
    private final boolean defenseSuccessful;
    private final int defeatedZombiesCount;
    private final int breachDamage;
    private final String summaryLog;

    public NightRaidResult(boolean attackOccurred, boolean defenseSuccessful,
                           int defeatedZombiesCount, int breachDamage, String summaryLog) {
        this.attackOccurred = attackOccurred;
        this.defenseSuccessful = defenseSuccessful;
        this.defeatedZombiesCount = defeatedZombiesCount;
        this.breachDamage = breachDamage;
        this.summaryLog = summaryLog;
    }

    public boolean isAttackOccurred() { return attackOccurred; }
    public boolean isDefenseSuccessful() { return defenseSuccessful; }
    public int getDefeatedZombiesCount() { return defeatedZombiesCount; }
    public int getBreachDamage() { return breachDamage; }
    public String getSummaryLog() { return summaryLog; }
}
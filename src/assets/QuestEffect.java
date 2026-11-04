package assets;

public enum QuestEffect {
    BOHOUS_OPRAVY, SLEVA_DODAVKA, TUNING_MOTORU, ARMADNI_TECHNIKA, SLEVA_ZBRANE_20,
    KONEC_UNIK, KONEC_LEK;

    public boolean isEnding() {
        return this == KONEC_UNIK || this == KONEC_LEK;
    }
}
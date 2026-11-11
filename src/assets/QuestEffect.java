package assets;

// Specialni odmeny za ukoly. Co ktery efekt dela, resi GameManager.onQuestEffect
// a ShopManager (slevy, odemceni zbozi).
public enum QuestEffect {
    BOHOUS_OPRAVY,      // Bohous opravuje elektriku po zaplavach
    SLEVA_DODAVKA,      // dodavka za 220 Kc misto 300
    TUNING_MOTORU,      // cesta do Mesta o 5 energie levnejsi
    ARMADNI_TECHNIKA,   // odemkne elektrickou ohradu a turrety v Army shopu
    SLEVA_ZBRANE_20,    // puska a brokovnice o 20 % levnejsi
    KONEC_UNIK,         // 1. vitezny konec (Viktor)
    KONEC_LEK;          // 2. vitezny konec (Elena)

    public boolean isEnding() {
        return this == KONEC_UNIK || this == KONEC_LEK;
    }
}

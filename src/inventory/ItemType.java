package inventory;

public enum ItemType {
    SEED("Semínko"),
    CROP("Plodina"),
    FOOD("Jídlo"),
    WEAPON("Zbraň"),
    DEFENSE_PASSIVE("Pasivní obrana"),
    DOG_FOOD("Krmení"),
    UPGRADE("Vylepšení"),
    VEHICLE("Dopravní prostředek");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
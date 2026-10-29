package map;

public enum TransportType {
    FOOT(0, "Pěšky"),
    BIKE(1, "Kolo"),
    CAR(2, "Auto"),
    VAN(3, "Dodávka");

    private final int level;
    private final String displayName;

    TransportType(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
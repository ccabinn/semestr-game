package map;

public enum TransportType {
    FOOT(0),
    BIKE(1),
    CAR(2),
    VAN(3);

    private final int level;

    TransportType(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
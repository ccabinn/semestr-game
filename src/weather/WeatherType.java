package weather;

public enum WeatherType {
    NORMAL("Slunečno / Normální"),
    DROUGHT("Sucho (vyšší spotřeba energie na zalévání)"),
    FLOOD("Záplava (elektrická zařízení zkratována)");

    private final String description;

    WeatherType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
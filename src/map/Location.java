package map;

import java.util.HashMap;
import java.util.Map;

public class Location {
    private final String name;
    private final String description;
    private final Map<String, Passage> passages = new HashMap<>();

    public Location(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void addPassage(String exitName, Passage passage) {
        passages.put(exitName.toLowerCase(), passage);
    }

    public Passage getPassage(String exitName) {
        return passages.get(exitName.toLowerCase());
    }

    public Map<String, Passage> getPassages() {
        return passages;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

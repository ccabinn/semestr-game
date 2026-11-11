package inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Item {

    private String id;
    private String name;
    private ItemType type;
    private int price;
    private int value;
    private String description;
    private List<String> availableLocations = new ArrayList<>();

    public Item() {
    }

    public Item(String id, String name, ItemType type, int price, int value, String description, List<String> availableLocations) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.value = value;
        this.description = description;
        this.availableLocations = availableLocations != null ? availableLocations : new ArrayList<>();
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public ItemType getType() { return type; }

    public int getPrice() { return price; }

    public int getValue() { return value; }

    public String getDescription() { return description; }

    public List<String> getAvailableLocations() {
        return availableLocations != null ? Collections.unmodifiableList(availableLocations) : Collections.emptyList();
    }
}
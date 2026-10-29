package assets;

public class Item {
    private final String id;
    private final String name;
    private final ItemType type;
    private final int price;
    private final int value;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemType getType() {
        return type;
    }

    public int getPrice() {
        return price;
    }

    public int getValue() {
        return value;
    }

    public Item(String id, String name, ItemType type, int price, int value) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.value = value;
    }
}

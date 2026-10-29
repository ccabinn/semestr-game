package assets;

public class Requirement {
    private final String itemId;
    private final int itemCount;

    public String getItemId() {
        return itemId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public Requirement(String itemId, int itemCount) {
        this.itemId = itemId;
        this.itemCount = itemCount;
    }

    @Override
    public String toString() {
        return itemCount + "× " + itemId;
    }
}

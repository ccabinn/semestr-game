package assets;

import java.util.HashMap;
import java.util.Map;

public class ItemCatalog {
    private final Map<String, Item> items = new HashMap<>();

    public void add(Item item){
        items.put(item.getId(), item);
    }

    public Item get(String id) {
        return items.get(id);
    }

    public String getName(String id) {
        Item item = items.get(id);
        return item == null ? id : item.getName();
    }
}

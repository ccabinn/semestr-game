package inventory;

import loader.ItemLoader;

import java.util.Collections;
import java.util.Map;

public class ItemCatalog {

    private final Map<String, Item> catalog;

    public ItemCatalog() {
        this.catalog = ItemLoader.loadItemsFromJSON("/data/items.json");
    }

    public Item getItem(String id) {
        if (id == null) return null;
        return catalog.get(id.toLowerCase());
    }

    public boolean hasItem(String id) {
        if (id == null) return false;
        return catalog.containsKey(id.toLowerCase());
    }

    public Map<String, Item> getCatalog() {
        return Collections.unmodifiableMap(catalog);
    }
}
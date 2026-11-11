package inventory;

import loader.ItemLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // vraci seznam vsech dostupnych predmetu pro danou lokalitu pro trading
    public List<Item> getItemsForLocation(String locationId) {
        if (locationId == null) return Collections.emptyList();
        String locKey = locationId.toLowerCase();

        List<Item> result = new ArrayList<>();
        for (Item item : catalog.values()) {
            if (item.getAvailableLocations().contains(locKey)) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Map<String, Item> getCatalog() {
        return Collections.unmodifiableMap(catalog);
    }
}
package inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Inventory {

    private final Map<String, Integer> items = new HashMap<>();
    private final ItemCatalog itemCatalog;

    public Inventory(ItemCatalog itemCatalog) {
        this.itemCatalog = itemCatalog;
    }

    // pridani predmetu
    public boolean addItem(String itemId, int count) {
        if (itemId == null || count <= 0 || !itemCatalog.hasItem(itemId)) {
            return false; // pokud predmet neexistuje vraci false
        }

        String key = itemId.toLowerCase();
        items.put(key, items.getOrDefault(key, 0) + count);
        return true;
    }

    // odebrani predmetu
    public boolean removeItem(String itemId, int count) {
        if (itemId == null || count <= 0) return false;

        String key = itemId.toLowerCase();
        int currentCount = items.getOrDefault(key, 0);

        if (currentCount < count) {
            return false; // false pokud hrac nema dostatek
        }

        if (currentCount == count) {
            items.remove(key); // pokud spotrebujeme vse, smazeme klic
        } else {
            items.put(key, currentCount - count);
        }
        return true;
    }

    // kontrola poctu predmetu
    public boolean hasItem(String itemId, int count) {
        if (itemId == null) return false;
        return items.getOrDefault(itemId.toLowerCase(), 0) >= count;
    }

    // ziskani poctu predmetu
    public int getItemCount(String itemId) {
        if (itemId == null) return 0;
        return items.getOrDefault(itemId.toLowerCase(), 0);
    }

    public Map<String, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    // vypis pro hrace
    public String getFormattedInventory() {
        if (items.isEmpty()) {
            return "Inventář je prázdný.";
        }

        StringBuilder sb = new StringBuilder("=== INVENTÁŘ ===\n");
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            Item item = itemCatalog.getItem(entry.getKey());
            String name = (item != null) ? item.getName() : entry.getKey();
            sb.append("- ").append(name).append(": ").append(entry.getValue()).append(" ks\n");
        }
        return sb.toString().trim();
    }

    public ItemCatalog getItemCatalog() {
        return itemCatalog;
    }
}
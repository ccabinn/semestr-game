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

    // --- PŘIDÁNÍ PŘEDMĚTU ---
    public boolean addItem(String itemId, int count) {
        if (itemId == null || count <= 0 || !itemCatalog.hasItem(itemId)) {
            return false; // Předmět neexistuje v katalogu nebo je neplatný počet
        }

        String key = itemId.toLowerCase();
        items.put(key, items.getOrDefault(key, 0) + count);
        return true;
    }

    // --- ODEBRÁNÍ PŘEDMĚTU ---
    public boolean removeItem(String itemId, int count) {
        if (itemId == null || count <= 0) return false;

        String key = itemId.toLowerCase();
        int currentCount = items.getOrDefault(key, 0);

        if (currentCount < count) {
            return false; // Hráč nemá dostatek kusů
        }

        if (currentCount == count) {
            items.remove(key); // Při spotřebování všech kusů klíč smažeme
        } else {
            items.put(key, currentCount - count);
        }
        return true;
    }

    // --- KONTROLA A ZÍSKÁNÍ POČTU ---
    public boolean hasItem(String itemId, int count) {
        if (itemId == null) return false;
        return items.getOrDefault(itemId.toLowerCase(), 0) >= count;
    }

    public int getItemCount(String itemId) {
        if (itemId == null) return 0;
        return items.getOrDefault(itemId.toLowerCase(), 0);
    }

    public Map<String, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    // --- VÝPIS PRO HRÁČE ---
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
}
package farming;

import loader.CropLoader;

import java.util.Collections;
import java.util.Map;

public class CropCatalog {

    private final Map<String, Crop> catalog;

    public CropCatalog() {
        this.catalog = CropLoader.loadCropsFromJSON("/data/crops.json");
    }

    public Crop getCrop(String query) {
        if (query == null) return null;
        String key = query.toLowerCase().trim();

        // hledani podle ID (napr wheat)
        if (catalog.containsKey(key)) {
            return catalog.get(key);
        }

        // hledani podle seedItemId nebo cropItemId (napr seminko_obili nebo obili)
        return catalog.values().stream()
                .filter(crop -> crop.getSeedItemId().equalsIgnoreCase(key)
                        || crop.getCropItemId().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    public Map<String, Crop> getCatalog() {
        return Collections.unmodifiableMap(catalog);
    }
}
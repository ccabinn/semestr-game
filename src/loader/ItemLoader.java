package loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import inventory.Item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemLoader {

    public static Map<String, Item> loadItemsFromJSON(String resourcePath) {
        Map<String, Item> itemMap = new HashMap<>();

        try (InputStream is = ItemLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Soubor s předměty nebyl nalezen v data: " + resourcePath);
                return itemMap;
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Item>>(){}.getType();
                List<Item> items = gson.fromJson(reader, listType);

                if (items != null) {
                    for (Item item : items) {
                        itemMap.put(item.getId().toLowerCase(), item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Chyba při načítání katalogu předmětů: " + e.getMessage());
        }

        return itemMap;
    }
}
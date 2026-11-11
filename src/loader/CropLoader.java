package loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import farming.Crop;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropLoader {

    public static Map<String, Crop> loadCropsFromJSON(String resourcePath) {
        Map<String, Crop> cropMap = new HashMap<>();

        try (InputStream is = CropLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Soubor s plodinami nebyl nalezen v resources: " + resourcePath);
                return cropMap;
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Crop>>(){}.getType();
                List<Crop> crops = gson.fromJson(reader, listType);

                if (crops != null) {
                    for (Crop crop : crops) {
                        cropMap.put(crop.getId().toLowerCase(), crop);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Chyba při načítání plodin přes Gson: " + e.getMessage());
        }

        return cropMap;
    }
}

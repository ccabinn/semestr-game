package loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import map.Location;
import map.Passage;
import map.TransportType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapLoader {

    private static class LocationDTO {
        public String id;
        public String name;
        public String description;
        public List<PassageDTO> passages;
    }

    private static class PassageDTO {
        public String targetId;
        public String requiredTransport;
        public int bikeEnergy;
        public int carEnergy;
        public int vanEnergy;
    }

    public static Map<String, Location> loadMapFromJSON(String resourcePath) {
        Map<String, Location> locationMap = new HashMap<>();

        try (InputStream is = MapLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Soubor s mapou nebyl nalezen v data: " + resourcePath);
                return locationMap;
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<LocationDTO>>(){}.getType();
                List<LocationDTO> dtos = gson.fromJson(reader, listType);

                // nacteni lokalit
                for (LocationDTO dto : dtos) {
                    locationMap.put(dto.id, new Location(dto.name, dto.description));
                }

                // nacteni passages
                for (LocationDTO dto : dtos) {
                    Location source = locationMap.get(dto.id);
                    if (source != null && dto.passages != null) {
                        for (PassageDTO pDto : dto.passages) {
                            Location target = locationMap.get(pDto.targetId);
                            TransportType transport = TransportType.valueOf(pDto.requiredTransport);

                            Passage passage = new Passage(
                                    target,
                                    transport,
                                    pDto.bikeEnergy,
                                    pDto.carEnergy,
                                    pDto.vanEnergy
                            );
                            source.addPassage(pDto.targetId, passage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Chyba při načítání mapy: " + e.getMessage());
        }

        return locationMap;
    }
}
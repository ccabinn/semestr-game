package save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// Uklada hry jako JSON soubory do slozky (vychozi: "saves" vedle spustene hry).
// Kazde ulozeni = jeden soubor <nazev>.json
public class JsonGameStorage implements GameStorage {

    private static final String EXTENSION = ".json";
    // povolene nazvy: mala pismena, cisla, _ a -, max 30 znaku (zadne "../" ani lomitka)
    private static final String VALID_NAME = "[a-z0-9_-]{1,30}";

    private final Path directory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonGameStorage(Path directory) {
        this.directory = directory;
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches(VALID_NAME);
    }

    @Override
    public void save(String name, GameState state) throws IOException {
        Files.createDirectories(directory); // pri prvnim ulozeni slozka jeste neexistuje
        try (Writer writer = Files.newBufferedWriter(fileFor(name), StandardCharsets.UTF_8)) {
            gson.toJson(state, writer);
        }
    }

    @Override
    public GameState load(String name) throws IOException {
        Path file = fileFor(name);
        if (!Files.exists(file)) {
            throw new IOException("Uložená hra '" + name + "' neexistuje.");
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            GameState state = gson.fromJson(reader, GameState.class);
            if (state == null) {
                throw new IOException("Soubor s uloženou hrou je prázdný.");
            }
            return state;
        } catch (JsonParseException e) {
            // rucne upraveny nebo poskozeny soubor
            throw new IOException("Soubor s uloženou hrou je poškozený: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listSaves() throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        // Stream API: projdi soubory ve slozce -> jen .json -> nazev bez pripony -> serad
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(EXTENSION))
                    .map(fileName -> fileName.substring(0, fileName.length() - EXTENSION.length()))
                    .sorted()
                    .toList();
        }
    }

    private Path fileFor(String name) throws IOException {
        if (!isValidName(name)) {
            throw new IOException("Neplatný název uložení '" + name + "'. Povolená jsou malá písmena, čísla, _ a -.");
        }
        return directory.resolve(name + EXTENSION);
    }
}

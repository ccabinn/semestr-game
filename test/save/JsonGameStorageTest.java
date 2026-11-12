package save;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Testy samotneho ukladani do JSON souboru (bez herni logiky).
class JsonGameStorageTest {

    @TempDir
    Path tempDir; // JUnit vytvori docasnou slozku a po testu ji smaze

    private JsonGameStorage storage;

    @BeforeEach
    void setUp() {
        storage = new JsonGameStorage(tempDir);
    }

    // jednoduchy stav hry pro testy
    private GameState sampleState() {
        return new GameState(GameState.CURRENT_VERSION, 7, "field", 55, 123, "CAR", true,
                List.of("wheat"), Map.of("chleb", 2),
                List.of(new GameState.PlotState("wheat", 1, true, false, false)),
                2, new GameState.WeatherState("DROUGHT", 25, 1, 3), 4, List.of("elena:0=DONE"));
    }

    @Test
    void savedStateLoadsBackEqual() throws IOException {
        GameState original = sampleState();
        storage.save("test", original);
        assertEquals(original, storage.load("test")); // record porovnava vsechna pole
    }

    @Test
    void loadingMissingSaveThrows() {
        assertThrows(IOException.class, () -> storage.load("neexistuje"));
    }

    @Test
    void invalidNameIsRejected() {
        assertThrows(IOException.class, () -> storage.save("../hack", sampleState()));
        assertThrows(IOException.class, () -> storage.save("Velka Pismena", sampleState()));
    }

    @Test
    void corruptedFileThrowsIOException() throws IOException {
        Files.writeString(tempDir.resolve("rozbite.json"), "{ tohle neni json");
        assertThrows(IOException.class, () -> storage.load("rozbite"));
    }

    @Test
    void listSavesReturnsSortedNamesWithoutExtension() throws IOException {
        storage.save("zebra", sampleState());
        storage.save("alfa", sampleState());
        Files.writeString(tempDir.resolve("poznamka.txt"), "neni to ulozeni");
        assertEquals(List.of("alfa", "zebra"), storage.listSaves());
    }

    @Test
    void listSavesOnMissingFolderIsEmpty() throws IOException {
        JsonGameStorage noFolder = new JsonGameStorage(tempDir.resolve("neexistuje"));
        assertTrue(noFolder.listSaves().isEmpty());
    }
}

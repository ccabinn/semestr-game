package save;

import java.io.IOException;
import java.util.List;

// Rozhrani pro ukladani hry. GameManager zna jen tohle rozhrani, ne konkretni format,
// takze jde snadno pridat jinou implementaci (napr. CSV, databaze, nebo "falesne" ulozisti v testech).
public interface GameStorage {

    // ulozi stav hry pod danym nazvem (existujici ulozeni prepise)
    void save(String name, GameState state) throws IOException;

    // nacte stav hry; vyhodi IOException, kdyz ulozeni neexistuje nebo je poskozene
    GameState load(String name) throws IOException;

    // nazvy vsech ulozenych her, serazene podle abecedy
    List<String> listSaves() throws IOException;
}

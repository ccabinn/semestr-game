package assets;

// Rozhrani, pres ktere NPC a ukoly pracuji se hrou
// Implementuje ho GameManager, takze balicek assets nemusi znat Player ani Inventory.
public interface GameContext {
    String getCurrentLocationId();
    void print(String text);
    int countItem(String itemId);
    void removeItem(String itemId, int count);
    void addItem(String itemId, int count);
    void addMoney(int amount);
    void removeMoney(int amount);
    int getMoney();

    // zavola se po splneni ukolu pro kazdy jeho efekt (sleva, odemceni, konec hry...)
    void onQuestEffect(QuestEffect effect);
}

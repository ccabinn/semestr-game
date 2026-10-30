package assets;

public interface GameContext {
    String getCurrentLocationId();
    void print(String text);
    int countItem(String itemId);
    void removeItem(String itemId, int count);
    void addMoney(int amount);
    int getMoney();
}
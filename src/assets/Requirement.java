package assets;

import inventory.Item;
import inventory.ItemCatalog;

import java.util.List;

// Jeden pozadavek ukolu: "N kusu jednoho z techto predmetu", nebo penize (id "penize").
// Vic ID v seznamu znamena "nebo" (napr. polevka nebo kolac).
public class Requirement {
    public static final String MONEY = "penize";

    private final List<String> itemIds;
    private final int itemCount;

    public Requirement(List<String> itemIds, int itemCount) {
        this.itemIds = itemIds;
        this.itemCount = itemCount;
    }

    public boolean isMoney() {
        return itemIds.size() == 1 && itemIds.get(0).equals(MONEY);
    }


    // ma hrac dost penez / aspon jeden z predmetu v potrebnem poctu?
    public boolean isMetBy(GameContext ctx) {
        if (isMoney()) {
            return ctx.getMoney() >= itemCount;
        }
        for (String id : itemIds) {
            if (ctx.countItem(id) >= itemCount) {
                return true;
            }
        }
        return false;
    }

    // odebere hraci pozadovane veci (u "nebo" ten prvni predmet, ktery ma)
    public void takeFrom(GameContext ctx) {
        if (isMoney()) {
            ctx.removeMoney(itemCount); // pozor: Player.addMoney zaporna cisla ignoruje
            return;
        }
        for (String id : itemIds) {
            if (ctx.countItem(id) >= itemCount) {
                ctx.removeItem(id, itemCount);
                return;
            }
        }
        throw new IllegalStateException("Požadavek není splněn: " + this);
    }

    // citelny popis pro hrace, napr. "1× Polévka nebo Koláč"
    public String describe(ItemCatalog catalog) {
        if (isMoney()) return itemCount + " Kč";
        StringBuilder sb = new StringBuilder(itemCount + "× ");
        for (int i = 0; i < itemIds.size(); i++) {
            if (i > 0) sb.append(" nebo ");
            Item item = catalog.getItem(itemIds.get(i));
            sb.append(item != null ? item.getName() : itemIds.get(i));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (isMoney()) {
            return itemCount + " Kč";
        }
        return itemCount + "× " + String.join(" nebo ", itemIds);
    }
}

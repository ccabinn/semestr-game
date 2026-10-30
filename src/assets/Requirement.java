package assets;

import java.util.List;

public class Requirement {
    public static final String MONEY = "penize";

    private final List<String> itemIds;
    private final int itemCount;

    public boolean isMoney() {
        return itemIds.size() == 1 && itemIds.get(0).equals(MONEY);
    }

    public List<String> getItemIds() { return itemIds; }

    public String getItemId() { return itemIds.get(0); }

    public int getItemCount() {
        return itemCount;
    }

    public Requirement(List<String> itemIds, int itemCount) {
        this.itemIds = itemIds;
        this.itemCount = itemCount;
    }

    public Requirement(String itemId, int itemCount) {
        this(List.of(itemId), itemCount);
    }

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

    public void takeFrom(GameContext ctx) {
        if (isMoney()) {
            ctx.addMoney(-itemCount);
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



    @Override
    public String toString() {
        if (isMoney()) {
            return itemCount + " Kč";
        }
        return itemCount + "× " + String.join(" nebo ", itemIds);
    }
}

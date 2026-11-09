package inventory;

import player.Player;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ShopManager {

    private final ItemCatalog itemCatalog;

    public ShopManager(ItemCatalog itemCatalog) {
        this.itemCatalog = itemCatalog;
    }

    // nakup predmetu
    public String buyItem(String itemId, int count, Player player) {
        if (count <= 0) {
            return "Počet kusů musí být větší než 0.";
        }

        Item item = itemCatalog.getItem(itemId);
        if (item == null) {
            return "Předmět '" + itemId + "' neexistuje v nabídce.";
        }

        String currentLocationId = player.getCurrentLocation().getId().toLowerCase();

        // kontrola zda se predmet v lokalite prodava
        if (!item.getAvailableLocations().contains(currentLocationId)) {
            return "Předmět " + item.getName() + " se v lokalitě " + player.getCurrentLocation().getName() + " neprodává!";
        }

        // kontrola na ovocna seminka
        if (item.getType() == ItemType.SEED && isFruitSeed(item.getId()) && !player.canPlantFruit()) {
            return "Tato semínka zatím nemůžeš koupit! Musíš nejprve zasadit všech 8 druhů zeleniny.";
        }

        int totalPrice = item.getPrice() * count;

        // kontrola penez
        if (player.getMoney() < totalPrice) {
            return "Nemáš dostatek peněz! Potřebuješ " + totalPrice + " Kč, ale máš jen " + player.getMoney() + " Kč.";
        }

        // samotny trade
        player.removeMoney(totalPrice);
        player.getInventory().addItem(item.getId(), count);

        return "Koupil jsi " + count + "x " + item.getName() + " za " + totalPrice + " Kč.";
    }

    // prodej predmetu
    public String sellItem(String itemId, int count, Player player) {
        if (count <= 0) {
            return "Počet kusů musí být větší než 0.";
        }

        Item item = itemCatalog.getItem(itemId);
        if (item == null) {
            return "Předmět '" + itemId + "' neexistuje.";
        }

        String currentLocationId = player.getCurrentLocation().getId().toLowerCase();

        if (!currentLocationId.equals("local_market") && !currentLocationId.equals("city_market")) {
            return "Prodávat zboží můžeš pouze na Místním nebo Městském trhu!";
        }

        if (!player.getInventory().hasItem(item.getId(), count)) {
            return "Nemáš v inventáři " + count + "x " + item.getName() + "!";
        }

        int unitPrice;

        if (item.getType() == ItemType.CROP) {
            // Plodiny: 100% základní cena na místním trhu, 200% (2x) na městském trhu
            unitPrice = item.getPrice();
            if (currentLocationId.equals("city_market")) {
                unitPrice *= 2;
            }
        } else {
            // Ostatní předměty (semínka, jídlo, zbraně...): výkup za 80 % nákupní ceny (bez městského násobku)
            unitPrice = (int) Math.round(item.getPrice() * 0.8);
        }

        int totalPrice = unitPrice * count;

        player.getInventory().removeItem(item.getId(), count);
        player.addMoney(totalPrice);

        return "Prodal jsi " + count + "x " + item.getName() + " za " + totalPrice + " Kč (" + unitPrice + " Kč/ks).";
    }
    public String getShopOffers(Player player) {
        String locId = player.getCurrentLocation().getId();
        List<Item> availableItems = new ArrayList<>(itemCatalog.getItemsForLocation(locId));

        if (availableItems.isEmpty()) {
            return "V lokalitě " + player.getCurrentLocation().getName() + " se nenachází žádný obchod.";
        }

        Collator czechCollator = Collator.getInstance(Locale.of("cs", "CZ"));

        // razeni podle typu, dale podle abecedy
        availableItems.sort(Comparator
                .comparing(Item::getType)
                .thenComparing((item1, item2) -> czechCollator.compare(item1.getName(), item2.getName()))
        );

        StringBuilder sb = new StringBuilder("=== NABÍDKA OBCHODU (" + player.getCurrentLocation().getName() + ") ===");
        ItemType lastType = null;

        for (Item item : availableItems) {
            if (item.getType() != lastType) {
                lastType = item.getType();
                sb.append("\n\n--- ").append(lastType.getDisplayName().toUpperCase()).append(" ---");
            }

            sb.append("\n- ").append(item.getName())
                    .append(" [ID: ").append(item.getId()).append("]")
                    .append(" - ").append(item.getPrice()).append(" Kč")
                    .append(" (").append(item.getDescription()).append(")");
        }

        return sb.toString();
    }

    private boolean isFruitSeed(String seedId) {
        return seedId.contains("jahody") || seedId.contains("maliny") || seedId.contains("boruvky")
                || seedId.contains("jablka") || seedId.contains("hrusky") || seedId.contains("merunky")
                || seedId.contains("broskev") || seedId.contains("meloun");
    }
}
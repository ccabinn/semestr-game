package farming;

import inventory.Item;
import inventory.ItemCatalog;
import player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldManager {

    private final List<Plot> plots = new ArrayList<>();
    private final CropCatalog cropCatalog;
    private int sprinklerCount = 0; // max 4 kropitka

    public FieldManager(CropCatalog cropCatalog) {
        this.cropCatalog = cropCatalog;
        for (int i = 1; i <= 6; i++) { // 6 zahonku
            plots.add(new Plot(i));
        }
    }

    // textovy prehled zahonu na poli
    public String getFieldStatus(ItemCatalog itemCatalog, boolean isDrought, boolean isFlooded) {
        if (plots == null || plots.isEmpty()) {
            return "Na poli nejsou žádné záhonky.";
        }

        StringBuilder sb = new StringBuilder("=== STAV ZÁHONKŮ NA POLI ===\n");
        for (Plot plot : plots) {
            sb.append("Záhonek č. ").append(plot.getIndex()).append(": ");

            if (plot.isEmpty()) {
                sb.append("Prázdný\n");
            } else if (plot.isWithered()) {
                String cropName = getCropDisplayName(plot.getCrop(), itemCatalog);
                sb.append(cropName).append(" | USCHLO (nezalito včas)\n");
            } else {
                String cropName = getCropDisplayName(plot.getCrop(), itemCatalog);

                // stav zalevani
                String waterStatus;
                if (isFlooded) {
                    waterStatus = "Zatopeno (Není potřeba zalévat)";
                } else if (plot.isWateredToday()) {
                    waterStatus = "Zalito (dnes)";
                } else if (plot.requiresWateringToday(isDrought, false)) {
                    waterStatus = "NUTNO ZALÍT!";
                } else {
                    waterStatus = "V pořádku (stačí zítra)";
                }

                sb.append(cropName)
                        .append(" | Zalévání: ").append(waterStatus)
                        .append(" | Stav: ");

                if (plot.isMature()) {
                    sb.append("PŘIPRAVENO KE SKLIZNI!\n");
                } else {
                    int daysLeft = plot.getDaysUntilHarvest();
                    sb.append("Zbývá ").append(daysLeft)
                            .append(daysLeft == 1 ? " den" : (daysLeft >= 2 && daysLeft <= 4 ? " dny" : " dní")).append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String getCropDisplayName(Crop crop, ItemCatalog itemCatalog) {
        if (crop == null) return "Neznámá plodina";
        if (itemCatalog != null) {
            Item item = itemCatalog.getItem(crop.getCropItemId());
            if (item != null) return item.getName();
        }
        return crop.getId();
    }

    public String plantCrop(String cropQuery, Player player, ItemCatalog itemCatalog) {
        Crop crop = cropCatalog.getCrop(cropQuery);
        if (crop == null) {
            return "Neznámá plodina nebo semínko: " + cropQuery;
        }

        // ovoce az po zasazeni vsech 8 druhů zeleniny
        if (crop.getCategory() == CropCategory.FRUIT && !player.canPlantFruit()) {
            return "Ovoce zatím nemůžeš sázet! Musíš nejprve zasadit všech 8 druhů zeleniny.";
        }

        String seedId = crop.getSeedItemId();
        if (!player.getInventory().hasItem(seedId, 1)) {
            Item seedItem = itemCatalog.getItem(seedId);
            String seedName = (seedItem != null) ? seedItem.getName() : seedId;
            return "Nemáš v inventáři: " + seedName;
        }

        Plot emptyPlot = null;
        for (Plot p : plots) {
            if (p.isEmpty()) {
                emptyPlot = p;
                break;
            }
        }

        if (emptyPlot == null) {
            return "Všechny záhonky jsou obsazené!";
        }

        player.getInventory().removeItem(seedId, 1);
        emptyPlot.plant(crop);

        boolean isVegetable = (crop.getCategory() == CropCategory.VEGETABLE);
        player.registerPlantedCrop(crop.getId(), isVegetable);

        Item cropItem = itemCatalog.getItem(crop.getCropItemId());
        String cropName = (cropItem != null) ? cropItem.getName() : crop.getId();

        return "Zasadil jsi " + cropName + " na záhonek č. " + emptyPlot.getIndex() + ".";
    }

    public String tendPlots(Player player, boolean isDrought, boolean isFlooded) {
        if (isFlooded) {
            return "Záhonky jsou zatopené, není potřeba plýtvat energií na zalévání.";
        }

        int baseCost = isDrought ? 10 : 5;
        int costPerPlot = Math.max(1, baseCost - sprinklerCount);

        int wateredCount = 0;
        int clearedCount = 0;
        int totalEnergyCost = 0;

        for (Plot p : plots) {
            if (p.isWithered()) {
                p.clear();
                clearedCount++;
                continue;
            }

            // zalevani zivych plodin
            if (!p.isEmpty() && !p.isWateredToday() && p.requiresWateringToday(isDrought, false)) {
                if (player.getEnergy() < totalEnergyCost + costPerPlot) {
                    break;
                }
                p.water();
                wateredCount++;
                totalEnergyCost += costPerPlot;
            }
        }

        // vyhodnoceni
        if (wateredCount == 0 && clearedCount == 0) {
            return "Žádné záhonky nevyžadovaly zalití a nebylo co vytrhávat.";
        }

        StringBuilder sb = new StringBuilder();
        if (wateredCount > 0) {
            player.removeEnergy(totalEnergyCost);
            sb.append("Zalil jsi ").append(wateredCount).append(" záhonků (Spotřebováno ")
                    .append(totalEnergyCost).append(" EN). ");
        }
        if (clearedCount > 0) {
            sb.append("Vytrhal jsi ").append(clearedCount).append(" uschlých plodin.");
        }

        return sb.toString().trim();
    }


    public String harvestAll(Player player, ItemCatalog itemCatalog) {
        int harvestedCount = 0;
        int clearedCount = 0;
        StringBuilder sb = new StringBuilder("Sklizeň:\n");

        for (Plot p : plots) {
            // cisteni uschlych plodin
            if (p.isWithered()) {
                p.clear();
                clearedCount++;
            }
            if (p.isMature()) {
                Crop harvested = p.harvest();
                player.getInventory().addItem(harvested.getCropItemId(), 1);

                Item itemInfo = itemCatalog.getItem(harvested.getCropItemId());
                String name = (itemInfo != null) ? itemInfo.getName() : harvested.getCropItemId();

                sb.append("- Sklizeno: ").append(name).append(" ze záhonku č. ").append(p.getIndex()).append("\n");
                harvestedCount++;
            }
        }

        if (harvestedCount == 0) {
            return "Žádné plodiny nejsou připraveny ke sklizni.";
        }

        if (clearedCount > 0) {
            sb.append("- Vytrháno uschlých plodin: ").append(clearedCount).append("\n");
        }

        return sb.toString().trim();
    }

    public void advanceDay(boolean isDrought, boolean isFlooded) {
        for (Plot p : plots) {
            p.advanceDay(isDrought, isFlooded);
        }
    }

    public boolean addSprinkler() {
        if (sprinklerCount < 4) {
            sprinklerCount++;
            return true;
        }
        return false;
    }

    public List<Plot> getPlots() { return Collections.unmodifiableList(plots); }
    public int getSprinklerCount() { return sprinklerCount; }
}
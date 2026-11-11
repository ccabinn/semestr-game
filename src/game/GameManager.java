package game;

import assets.GameContext;
import assets.NpcManager;
import assets.QuestEffect;
import farming.CropCatalog;
import farming.FieldManager;
import weather.WeatherManager;

import zombies.DefenseStatus;
import zombies.NightRaidResult;
import zombies.ZombieManager;
import inventory.ItemCatalog;
import inventory.ShopManager;
import map.GamePlan;
import loader.NpcLoader;
import map.Location;
import player.Player;

import java.util.Map;

public class GameManager implements GameContext {

    private final GamePlan gamePlan;
    private final Player player;
    private final FieldManager fieldManager;
    private final ShopManager shopManager;
    private final ItemCatalog itemCatalog;
    private final CropCatalog cropCatalog;

    private final ZombieManager zombieManager;
    private final WeatherManager weatherManager;
    private DefenseStatus defenseStatus;
    private final NpcManager npcManager;

    // sem pise NpcManager pres print(), processCommand to pak vrati jako text
    private final StringBuilder output = new StringBuilder();

    private int currentDay = 1;
    private boolean isGameOver = false;

    public GameManager() {
        this.itemCatalog = new ItemCatalog();
        this.cropCatalog = new CropCatalog();
        this.gamePlan = new GamePlan();

        Location startLocation = gamePlan.getLocationById("home");
        this.player = new Player(startLocation, itemCatalog);
        this.fieldManager = new FieldManager(cropCatalog);
        this.npcManager = NpcLoader.loadNpcsFromJSON("/data/npcs.json", itemCatalog);
        this.shopManager = new ShopManager(itemCatalog, npcManager);

        this.zombieManager = new ZombieManager();
        this.weatherManager = new WeatherManager();
        this.defenseStatus = new DefenseStatus(0, 0, 0, 0, 0, false, false);

        // startovni vybava podle GDD
        player.getInventory().addItem("seminko_obili", 2);
    }

    // zpracovani prikazu
    public String processCommand(String inputLine) {
        if (inputLine == null || inputLine.trim().isEmpty()) {
            return "Zadej platný příkaz. (Nápověda: 'help')";
        }

        String[] parts = inputLine.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        String param1 = parts.length > 1 ? parts[1].toLowerCase() : null;
        String param2 = parts.length > 2 ? parts[2].toLowerCase() : null;

        switch (command) {
            case "jdi":
            case "go":
            case "move":
                if (param1 == null) return "Kam chceš jít? Použití: jdi <id_lokality>";
                return gamePlan.movePlayer(player, param1);

            case "inventar":
            case "inv":
                return player.getInventory().getFormattedInventory();

            case "stav":
            case "status":
                return getPlayerStatus();

            case "obchod":
            case "nabidka":
                return shopManager.getShopOffers(player);

            case "kup":
            case "buy":
                if (param1 == null) return "Co chceš koupit? Použití: kup <id_predmetu> [pocet]";
                int buyCount = parseCount(param2);
                String buyResult = shopManager.buyItem(param1, buyCount, player);
                if (buyResult.startsWith("Koupil jsi") && param1.equalsIgnoreCase("krmeni_psy")) {
                    for (int i = 0; i < buyCount; i++) {
                        defenseStatus.feedDogs();
                    }
                }
                return buyResult;

            case "prodej":
            case "sell":
                if (param1 == null) return "Co chceš prodat? Použití: prodej <id_predmetu> [pocet]";
                int sellCount = parseCount(param2);
                return shopManager.sellItem(param1, sellCount, player);

            case "zasad":
            case "plant":
                if (param1 == null) return "Co chceš zasadit? Použití: zasad <id_plodiny/seminka>";
                if (!player.getCurrentLocation().getId().equalsIgnoreCase("field")) {
                    return "Sázet můžeš pouze na poli!";
                }
                return fieldManager.plantCrop(param1, player, itemCatalog);

            case "obstarej":
            case "zali":
            case "water":
                if (!player.getCurrentLocation().getId().equalsIgnoreCase("field")) {
                    return "Obstarávat záhonky můžeš pouze na poli!";
                }
                return fieldManager.tendPlots(player, weatherManager.isDrought());

            case "sklid":
            case "harvest":
                if (!player.getCurrentLocation().getId().equalsIgnoreCase("field")) {
                    return "Sklízet úrodu můžeš pouze na poli!";
                }
                return fieldManager.harvestAll(player, itemCatalog);

            case "spi":
            case "sleep":
                if (!player.getCurrentLocation().getId().equalsIgnoreCase("home")) {
                    return "Vyspat se do nového dne můžeš pouze v Domě!";
                }
                return advanceToNextDay();

            case "mluv":
            case "talk":
                if (param1 == null) return "S kým chceš mluvit? Použití: mluv <npc>";
                npcManager.talk(param1, this);
                return flushOutput();

            case "ukol":
                if (param1 == null) return "Komu chceš odevzdat úkol? Použití: ukol <npc>";
                npcManager.handIn(param1, this);
                return flushOutput();

            case "napoveda":
            case "pomoc":
            case "help":
                return getHelpText();

            default:
                return "Neznámý příkaz '" + command + "'. Napiš 'napoveda' pro seznam příkazů.";
        }
    }

    // ===== GameContext - napojeni NPC a ukolu na hrace =====

    @Override
    public String getCurrentLocationId() { return player.getCurrentLocation().getId(); }

    @Override
    public void print(String text) { output.append(text).append("\n"); }

    @Override
    public int countItem(String itemId) { return player.getInventory().getItemCount(itemId); }

    @Override
    public void removeItem(String itemId, int count) { player.getInventory().removeItem(itemId, count); }

    @Override
    public void addItem(String itemId, int count) { player.getInventory().addItem(itemId, count); }

    @Override
    public void addMoney(int amount) { player.addMoney(amount); }

    @Override
    public void removeMoney(int amount) { player.removeMoney(amount); }

    @Override
    public int getMoney() { return player.getMoney(); }

    // efekty, ktere meni stav hry hned. Slevy a odemceni v obchode
    // si ShopManager zjistuje sam pres npcManager.hasEffect(...)
    @Override
    public void onQuestEffect(QuestEffect effect) {
        switch (effect) {
            case TUNING_MOTORU:
                player.setHasEngineTuning(true);
                print("Motor je seřízený: cesty do Města tě stojí o 5 energie méně.");
                break;
            case SLEVA_DODAVKA:
                print("Dodávku teď v Autobazaru koupíš za 220 Kč.");
                break;
            case ARMADNI_TECHNIKA:
                print("V Army shopu si teď můžeš koupit elektrickou ohradu a turrety.");
                break;
            case SLEVA_ZBRANE_20:
                print("Zbraně v Army shopu máš o 20 % levnější.");
                break;
            case BOHOUS_OPRAVY:
                // TODO: az budou zaplavy zkratovat ohradu/turrety, pridat prikaz na opravu za 50 Kc
                print("Bohouš ti teď za 50 Kč opraví elektriku poničenou záplavami.");
                break;
            case KONEC_UNIK:
                isGameOver = true;
                print("*** VÍTĚZSTVÍ: Únik do bezpečí! Hra dokončena za " + currentDay + " dní. ***");
                break;
            case KONEC_LEK:
                isGameOver = true;
                print("*** VÍTĚZSTVÍ: Lék proti nákaze! Hra dokončena za " + currentDay + " dní. ***");
                break;
        }
    }

    // vrati vse, co se napsalo pres print(), a vycisti buffer
    private String flushOutput() {
        String text = output.toString().trim();
        output.setLength(0);
        return text;
    }

    // prechod na novy den
    public String advanceToNextDay() {
        StringBuilder sb = new StringBuilder();

        // aktualizace obranneho stavu podle inventare hrace
        syncDefenseStatusFromInventory();

        // vyhodnoceni nocniho najezdu zombies
        NightRaidResult raidResult = zombieManager.resolveNightRaid(
                currentDay,
                defenseStatus,
                weatherManager.isFlooded()
        );

        // doplneni energie
        if (raidResult.isAttackOccurred() && !raidResult.isDefenseSuccessful()) {
            sb.append(raidResult.getSummaryLog()).append("\n\n");
            sb.append("Z důvodu prolomení obrany se hráč v noci nemohl vyspat a NEDOPLNIL si energii!\n");
        } else {
            if (raidResult.isAttackOccurred()) {
                sb.append(raidResult.getSummaryLog()).append("\n\n");
            }
            player.addEnergy(50);
            sb.append("Doplněno +50 energie ze spánku.\n");
        }

        currentDay++;

        // posun rustu plodin na poli
        fieldManager.advanceDay(weatherManager.isDrought());
        sb.append("Plodiny na poli pokročily v růstu.\n\n");

        // vyhodnoceni pocasi pro nasledujici den
        String weatherReport = weatherManager.advanceWeather(currentDay);
        sb.append("=== PROBUDIL SES DO DNE ").append(currentDay).append(" ===\n");
        sb.append(weatherReport);

        return sb.toString().trim();
    }

    // vyhodnoceni obranneho stavu podle inventare
    private void syncDefenseStatusFromInventory() {
        Map<String, Integer> items = player.getInventory().getItems();

        int dogs = items.getOrDefault("pes", 0);
        int woodenFences = items.getOrDefault("plot", 0);
        int electricFences = items.getOrDefault("elektricka_ohrada", 0);
        int turrets = items.getOrDefault("turret", 0);
        boolean hasRifle = items.getOrDefault("puska", 0) > 0;
        boolean hasShotgun = items.getOrDefault("brokovnice", 0) > 0;

        int currentDogFoodDays = (defenseStatus != null) ? defenseStatus.getDogFoodDaysLeft() : 0;

        this.defenseStatus = new DefenseStatus(
                dogs,
                currentDogFoodDays,
                woodenFences,
                electricFences,
                turrets,
                hasRifle,
                hasShotgun
        );
    }

    public String getPlayerStatus() {
        return String.format(
                "=== STAV HRÁČE ===\n" +
                        "Den: %d | Lokalita: %s (ID: %s)\n" +
                        "Počasí: %s\n" +
                        "Energie: %d/100 EN | Peníze: %d Kč\n" +
                        "Vozidlo: %s | Motorový tuning: %s\n" +
                        "Zasazeno unikátní zeleniny: %d/8",
                currentDay,
                player.getCurrentLocation().getName(),
                player.getCurrentLocation().getId(),
                weatherManager.getCurrentWeather().getDescription(),
                player.getEnergy(),
                player.getMoney(),
                player.getBestVehicle().getDisplayName(),
                player.hasEngineTuning() ? "ANO" : "NE",
                player.getPlantedVegetablesCount()
        );
    }

    private String getHelpText() {
        return "=== SEZNAM PŘÍKAZŮ ===\n" +
                "- jdi <id_lokality>    : Přesun na sousední lokalitu (např. 'jdi field', 'jdi local_market')\n" +
                "- stav                  : Zobrazí celkový stav hráče, den a počasí\n" +
                "- inventar              : Zobrazí obsah batohu\n" +
                "- obchod                : Vypíše nabídku obchodu v aktuální lokalitě\n" +
                "- kup <id> [pocet]      : Nákup předmětu v obchodě (např. 'kup seminko_obili 3')\n" +
                "- prodej <id> [pocet]   : Prodej předmětu z inventáře\n" +
                "- zasad <id_plodiny>   : Zasazení semínka (pouze na poli)\n" +
                "- obstarej              : Zalití a obstarání záhonků na poli\n" +
                "- sklid                 : Sklizeň dozrálých plodin na poli\n" +
                "- spi                   : Spánek a posun do dalšího dne (pouze v Domě)\n" +
                "- mluv <npc>            : Rozhovor s postavou (např. 'mluv bohous')\n" +
                "- ukol <npc>            : Odevzdání úkolu postavě (např. 'ukol elena')\n" +
                "- napoveda              : Vypíše tuto nápovědu";
    }

    private int parseCount(String text) {
        if (text == null) return 1;
        try {
            int val = Integer.parseInt(text);
            return val > 0 ? val : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public Player getPlayer() { return player; }
    public GamePlan getGamePlan() { return gamePlan; }
    public FieldManager getFieldManager() { return fieldManager; }
    public ShopManager getShopManager() { return shopManager; }
    public ItemCatalog getItemCatalog() { return itemCatalog; }
    public CropCatalog getCropCatalog() { return cropCatalog; }
    public ZombieManager getZombieManager() { return zombieManager; }
    public WeatherManager getWeatherManager() { return weatherManager; }
    public NpcManager getNpcManager() { return npcManager; }
    public DefenseStatus getDefenseStatus() { return defenseStatus; }
    public int getCurrentDay() { return currentDay; }
    public boolean isGameOver() { return isGameOver; }
}
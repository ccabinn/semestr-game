package game;

import farming.CropCatalog;
import farming.FieldManager;
import inventory.Item;
import weather.WeatherManager;

import zombies.DefenseStatus;
import zombies.NightRaidResult;
import zombies.ZombieManager;
import inventory.ItemCatalog;
import inventory.ShopManager;
import map.GamePlan;
import map.Location;
import player.Player;

import java.util.List;
import java.util.Map;

public class GameManager {

    private final GamePlan gamePlan;
    private final Player player;
    private final FieldManager fieldManager;
    private final ShopManager shopManager;
    private final ItemCatalog itemCatalog;
    private final CropCatalog cropCatalog;

    private final ZombieManager zombieManager;
    private final WeatherManager weatherManager;
    private DefenseStatus defenseStatus;

    private int currentDay = 1;
    private boolean isGameOver = false;

    public GameManager() {
        this.itemCatalog = new ItemCatalog();
        this.cropCatalog = new CropCatalog();
        this.gamePlan = new GamePlan();

        Location startLocation = gamePlan.getLocationById("home");
        this.player = new Player(startLocation, itemCatalog);
        this.fieldManager = new FieldManager(cropCatalog);
        this.shopManager = new ShopManager(itemCatalog);

        this.zombieManager = new ZombieManager();
        this.weatherManager = new WeatherManager();
        this.defenseStatus = new DefenseStatus(0, 0, 0, 0, 0, false, false);
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

                if (buyResult.startsWith("Koupil jsi")) {
                    handlePostPurchaseEffects(param1, buyCount);
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

            case "napoveda":
            case "pomoc":
            case "help":
                return getHelpText();

            case "pole":
            case "zahony":
            case "fieldstatus":
                if (!player.getCurrentLocation().getId().equalsIgnoreCase("field")) {
                    return "Stav záhonků lze zobrazit pouze na poli!";
                }
                return fieldManager.getFieldStatus(itemCatalog, weatherManager.isDrought());

            // DEVTOOLS (pouze pres konzoli)
            case "tp":
            case "teleport":
                if (param1 == null) return "DEV: Zadej ID lokace (např. 'tp city_market', 'tp army_shop', 'tp home').";
                Location targetLoc = gamePlan.getLocationById(param1);
                if (targetLoc == null) return "DEV: Lokace s ID '" + param1 + "' neexistuje!";
                player.setCurrentLocation(targetLoc);
                return "DEV: Teleportován do lokace " + targetLoc.getName() + " (" + targetLoc.getId() + ").";

            case "give":
            case "additem":
                if (param1 == null) return "DEV: Použití: give <id_itemu> [pocet]";
                int giveCount = parseCount(param2);
                if (itemCatalog.getItem(param1) == null) return "DEV: Předmět '" + param1 + "' neexistuje v katalogu!";
                player.getInventory().addItem(param1, giveCount);
                return "DEV: Přidáno " + giveCount + "x " + param1 + " do inventáře.";

            case "addmoney":
            case "money":
                int moneyAmount = parseCount(param1);
                player.addMoney(moneyAmount);
                return "DEV: Přidáno " + moneyAmount + " Kč. Aktuální stav: " + player.getMoney() + " Kč.";

            case "addenergy":
            case "energy":
                int energyAmount = parseCount(param1);
                player.addEnergy(energyAmount);
                return "DEV: Doplněno +" + energyAmount + " EN. Aktuální stav: " + player.getEnergy() + "/100 EN.";

            case "setday":
                if (param1 == null) return "DEV: Zadej číslo dne (např. 'setday 10').";
                try {
                    int targetDay = Integer.parseInt(param1);
                    this.currentDay = Math.max(1, targetDay);
                    return "DEV: Den nastaven na " + currentDay + ".";
                } catch (NumberFormatException e) {
                    return "DEV: Neplatné číslo dne!";
                }

            case "setweather":
                if (param1 == null) return "DEV: Použití: setweather <fair|drought|flood>";
                return devSetWeather(param1);

            case "setvehicle":
                if (param1 == null) return "DEV: Použití: setvehicle <bike|car|van>";
                switch (param1) {
                    case "bike": player.setBestVehicle(map.TransportType.BIKE); break;
                    case "car": player.setBestVehicle(map.TransportType.CAR); break;
                    case "van": player.setBestVehicle(map.TransportType.VAN); break;
                    default: return "DEV: Neznámé vozidlo (možnosti: bike, car, van).";
                }
                return "DEV: Vozidlo změněno na " + player.getBestVehicle().getDisplayName() + ".";

            case "growall":
            case "dozraj":
                for (farming.Plot plot : fieldManager.getPlots()) {
                    if (!plot.isEmpty() && !plot.isWithered()) {
                        // Posuneme růst přímo na potřebný počet dní pro danou plodinu
                        int requiredDays = plot.getCrop().getGrowthDays();
                        while (!plot.isMature()) {
                            plot.advanceDay(false);
                            // Pojistka, aby zalití neblokovalo růst při ručním posunu
                            plot.water();
                        }
                    }
                }
                return "DEV: Všechny plodiny na poli okamžitě dozrály!";

            case "devhelp":
            case "dev":
            case "cheats":
                return getDevHelpText();

            case "raid":
            case "zombieraid":
                syncDefenseStatusFromInventory();
                zombies.NightRaidResult raidResult = zombieManager.resolveNightRaid(
                        currentDay,
                        defenseStatus,
                        weatherManager.isFlooded()
                );
                return "DEV: Testovací raid spuštěn!\n" + raidResult.getSummaryLog();

            case "items":
            case "predmety":
            case "listitems":
                return devListItems();

            default:
                return "Neznámý příkaz '" + command + "'. Napiš 'napoveda' pro seznam příkazů.";
        }
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

    // zpracovava vedlejsi herni efekty zakoupenych predmetu
    private void handlePostPurchaseEffects(String itemId, int count) {
        String id = itemId.toLowerCase();

        switch (id) {
            case "auto":
            case "car":
                if (player.getBestVehicle().getLevel() < map.TransportType.CAR.getLevel()) {
                    player.setBestVehicle(map.TransportType.CAR);
                }
                break;

            case "dodavka":
            case "van":
                if (player.getBestVehicle().getLevel() < map.TransportType.VAN.getLevel()) {
                    player.setBestVehicle(map.TransportType.VAN);
                }
                break;

            case "motorovy_tuning":
                player.setHasEngineTuning(true);
                break;

            case "kropitko":
                for (int i = 0; i < count; i++) {
                    fieldManager.addSprinkler();
                }
                break;

            case "hlubsi_studna":
                for (int i = 0; i < count; i++) {
                    weatherManager.addWellLevel();
                }
                break;

            // Obrana a pes
            case "krmeni_psy":
                for (int i = 0; i < count; i++) {
                    defenseStatus.feedDogs();
                }
                break;
        }
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
                "- napoveda              : Vypíše tuto nápovědu";
    }

    private String getDevHelpText() {
        return "=== DEVTOOLS / CHEATY (Pouze pro CLI) ===\n" +
                "- tp / teleport <id_lokace>           : Teleport na lokaci (např. 'tp city_market', 'tp home')\n" +
                "- give / additem <id_itemu> [pocet]   : Přidá předmět do inventáře (např. 'give seminko_obili 10')\n" +
                "- money / addmoney <castka>          : Přidá peníze hráči (např. 'money 5000')\n" +
                "- energy / addenergy <castka>        : Doplní energii (např. 'energy 50')\n" +
                "- setday <cislo_dne>                 : Posune hru na konkrétní den (např. 'setday 10')\n" +
                "- setweather <slunecno|sucho|zaplava>: Změní počasí (fair, drought, flood)\n" +
                "- setvehicle <bike|car|van>           : Změní nejlepší vozidlo hráče\n" +
                "- growall / dozraj                   : Okamžitě nechá dozrát všechny plodiny na poli\n" +
                "- raid / zombieraid                  : Okamžitě vyvolá noční přepad zombíků\n" +
                "- items / predmety                   : Vypíše seznam všech předmětů v hře seřazený podle kategorií a abecedně\n" +
                "- devhelp / dev / cheats             : Zobrazí tento seznam vývojářských příkazů";
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

    private String devSetWeather(String type) {
        if (type == null) return "DEV: Použití: setweather <fair|drought|flood>";

        switch (type.toLowerCase()) {
            case "fair":
            case "slunecno":
            case "normal":
                weatherManager.setCurrentWeather(weather.WeatherType.NORMAL);
                return "DEV: Počasí ručně změněno na Slunečno.";

            case "drought":
            case "sucho":
                weatherManager.setCurrentWeather(weather.WeatherType.DROUGHT);
                return "DEV: Počasí ručně změněno na Sucho.";

            case "flood":
            case "zaplava":
                weatherManager.setCurrentWeather(weather.WeatherType.FLOOD);
                return "DEV: Počasí ručně změněno na Záplavu.";

            default:
                return "DEV: Neznámý typ počasí! Možnosti: normal/slunecno, drought/sucho, flood/zaplava.";
        }
    }

    private String devListItems() {
        List<Item> allItems = new java.util.ArrayList<>(itemCatalog.getCatalog().values());
        if (allItems.isEmpty()) {
            return "DEV: Katalog předmětů je prázdný.";
        }

        java.text.Collator czechCollator = java.text.Collator.getInstance(java.util.Locale.of("cs", "CZ"));
        allItems.sort(java.util.Comparator
                .comparing(inventory.Item::getType)
                .thenComparing((a, b) -> czechCollator.compare(a.getName(), b.getName()))
        );

        StringBuilder sb = new StringBuilder("=== DEV: VŠECHNY PŘEDMĚTY V KATALOGU ===");
        inventory.ItemType lastType = null;

        for (inventory.Item item : allItems) {
            if (item.getType() != lastType) {
                lastType = item.getType();
                sb.append("\n\n--- ").append(lastType.getDisplayName().toUpperCase()).append(" ---");
            }
            sb.append(String.format("\n- ID: %-20s | %-22s | Cena: %d Kč",
                    item.getId(), item.getName(), item.getPrice()));
        }

        return sb.toString();
    }

    public Player getPlayer() { return player; }
    public GamePlan getGamePlan() { return gamePlan; }
    public FieldManager getFieldManager() { return fieldManager; }
    public ShopManager getShopManager() { return shopManager; }
    public ItemCatalog getItemCatalog() { return itemCatalog; }
    public CropCatalog getCropCatalog() { return cropCatalog; }
    public ZombieManager getZombieManager() { return zombieManager; }
    public WeatherManager getWeatherManager() { return weatherManager; }
    public DefenseStatus getDefenseStatus() { return defenseStatus; }
    public int getCurrentDay() { return currentDay; }
    public boolean isGameOver() { return isGameOver; }
}
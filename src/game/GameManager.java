package game;

import assets.GameContext;
import assets.NpcManager;
import assets.QuestEffect;
import farming.CropCatalog;
import farming.FieldManager;
import inventory.Item;
import inventory.ItemCatalog;
import inventory.ItemType;
import inventory.ShopManager;
import loader.NpcLoader;
import map.GamePlan;
import map.Location;
import map.TransportType;
import player.Player;
import weather.WeatherManager;
import weather.WeatherType;
import zombies.DefenseStatus;
import zombies.NightRaidResult;
import zombies.ZombieManager;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    private final NpcManager npcManager;
    private DefenseStatus defenseStatus;

    // Buffer pro zachycení výstupu rozhovorů a úkolů z NpcManageru
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

        // Startovní výbava podle GDD
        player.getInventory().addItem("seminko_obili", 2);
    }

    // =========================================================
    // SPRACOVÁNÍ PŘÍKAZŮ (MAIN ROUTING)
    // =========================================================

    public String processCommand(String inputLine) {
        if (inputLine == null || inputLine.trim().isEmpty()) {
            return "Zadej platný příkaz. (Nápověda: 'help')";
        }

        String[] parts = inputLine.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        String param1 = parts.length > 1 ? parts[1].toLowerCase() : null;
        String param2 = parts.length > 2 ? parts[2].toLowerCase() : null;

        return switch (command) {
            // Základní pohyb a stav
            case "jdi", "go", "move" -> (param1 == null) ? "Kam chceš jít? Použití: jdi <id_lokality>"
                    : gamePlan.movePlayer(player, param1);
            case "inventar", "inv" -> player.getInventory().getFormattedInventory();
            case "stav", "status" -> getPlayerStatus();
            case "napoveda", "pomoc", "help" -> getHelpText();
            case "spi", "sleep" -> handleSleep();

            // Obchod
            case "obchod", "nabidka" -> shopManager.getShopOffers(player);
            case "kup", "buy" -> handleBuy(param1, param2);
            case "prodej", "sell" -> handleSell(param1, param2);

            // Farming
            case "zasad", "plant", "obstarej", "zali", "water", "sklid", "harvest", "pole", "zahony", "fieldstatus" ->
                    handleFarmingCommand(command, param1);

            // NPC & Úkoly
            case "mluv", "talk", "ukol" -> handleNpcCommand(command, param1);

            // Vývojářské příkazy (DevTools)
            case "tp", "teleport", "give", "additem", "addmoney", "money", "addenergy", "energy",
                 "setday", "setweather", "setvehicle", "growall", "dozraj", "raid", "zombieraid",
                 "items", "predmety", "listitems", "devhelp", "dev", "cheats" -> handleDevCommand(command, param1, param2);

            default -> "Neznámý příkaz '" + command + "'. Napiš 'napoveda' pro seznam příkazů.";
        };
    }

    // =========================================================
    // HANDLERY JEDNOTLIVÝCH SEKCÍ PŘÍKAZŮ
    // =========================================================

    private String handleSleep() {
        if (!player.getCurrentLocation().getId().equalsIgnoreCase("home")) {
            return "Vyspat se do nového dne můžeš pouze v Domě!";
        }
        return advanceToNextDay();
    }

    private String handleBuy(String itemId, String countParam) {
        if (itemId == null) return "Co chceš koupit? Použití: kup <id_predmetu> [pocet]";
        int buyCount = parseCount(countParam);
        String buyResult = shopManager.buyItem(itemId, buyCount, player);

        if (buyResult.startsWith("Koupil jsi")) {
            handlePostPurchaseEffects(itemId, buyCount);
        }
        return buyResult;
    }

    private String handleSell(String itemId, String countParam) {
        if (itemId == null) return "Co chceš prodat? Použití: prodej <id_predmetu> [pocet]";
        int sellCount = parseCount(countParam);
        return shopManager.sellItem(itemId, sellCount, player);
    }

    private String handleFarmingCommand(String command, String param1) {
        if (!player.getCurrentLocation().getId().equalsIgnoreCase("field")) {
            return "Farmářské akce můžeš provádět pouze na poli!";
        }

        return switch (command) {
            case "zasad", "plant" -> (param1 == null) ? "Co chceš zasadit? Použití: zasad <id_plodiny/seminka>"
                    : fieldManager.plantCrop(param1, player, itemCatalog);
            case "obstarej", "zali", "water" -> fieldManager.tendPlots(player, weatherManager.isDrought(), weatherManager.isFlooded());
            case "sklid", "harvest" -> fieldManager.harvestAll(player, itemCatalog);
            case "pole", "zahony", "fieldstatus" -> fieldManager.getFieldStatus(itemCatalog, weatherManager.isDrought(), weatherManager.isFlooded());
            default -> "Neznámý farmářský příkaz.";
        };
    }

    private String handleNpcCommand(String command, String npcId) {
        if (npcId == null) {
            return command.equals("ukol") ? "Komu chceš odevzdat úkol? Použití: ukol <npc>"
                    : "S kým chceš mluvit? Použití: mluv <npc>";
        }

        if (command.equals("ukol")) {
            npcManager.handIn(npcId, this);
        } else {
            npcManager.talk(npcId, this);
        }
        return flushOutput();
    }

    // =========================================================
    // GAMECONTEXT INTERFACE (PROPOJENÍ S NPC A ÚKOLY)
    // =========================================================

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

    @Override
    public void onQuestEffect(QuestEffect effect) {
        switch (effect) {
            case TUNING_MOTORU -> {
                player.setHasEngineTuning(true);
                print("Motor je seřízený: cesty do Města tě stojí o 5 energie méně.");
            }
            case SLEVA_DODAVKA -> print("Dodávku teď v Autobazaru koupíš za 220 Kč.");
            case ARMADNI_TECHNIKA -> print("V Army shopu si teď můžeš koupit elektrickou ohradu a turrety.");
            case SLEVA_ZBRANE_20 -> print("Zbraně v Army shopu máš o 20 % levnější.");
            case BOHOUS_OPRAVY -> print("Bohouš ti teď za 50 Kč opraví elektriku poničenou záplavami.");
            case KONEC_UNIK -> {
                isGameOver = true;
                print("*** VÍTĚZSTVÍ: Únik do bezpečí! Hra dokončena za " + currentDay + " dní. ***");
            }
            case KONEC_LEK -> {
                isGameOver = true;
                print("*** VÍTĚZSTVÍ: Lék proti nákaze! Hra dokončena za " + currentDay + " dní. ***");
            }
        }
    }

    private String flushOutput() {
        String text = output.toString().trim();
        output.setLength(0);
        return text;
    }

    // prochazeni dni
    public String advanceToNextDay() {
        StringBuilder sb = new StringBuilder();

        NightRaidResult raidResult = zombieManager.resolveNightRaid(
                currentDay,
                defenseStatus,
                weatherManager.isFlooded()
        );

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

        fieldManager.advanceDay(weatherManager.isDrought(), weatherManager.isFlooded());
        sb.append("Plodiny na poli pokročily v růstu.\n\n");

        String weatherReport = weatherManager.advanceWeather(currentDay);
        sb.append("=== PROBUDIL SES DO DNE ").append(currentDay).append(" ===\n");
        sb.append(weatherReport);

        return sb.toString().trim();
    }

    // side effects nakupu
    private void handlePostPurchaseEffects(String itemId, int count) {
        String id = itemId.toLowerCase();
        switch (id) {
            case "auto", "car" -> {
                if (player.getBestVehicle().getLevel() < TransportType.CAR.getLevel()) {
                    player.setBestVehicle(TransportType.CAR);
                }
            }
            case "dodavka", "van" -> {
                if (player.getBestVehicle().getLevel() < TransportType.VAN.getLevel()) {
                    player.setBestVehicle(TransportType.VAN);
                }
            }
            case "motorovy_tuning" -> player.setHasEngineTuning(true);

            case "kropitko" -> {
                for (int i = 0; i < count; i++) fieldManager.addSprinkler();
            }
            case "hlubsi_studna" -> {
                for (int i = 0; i < count; i++) weatherManager.addWellLevel();
            }
            case "krmeni_psy" -> {
                for (int i = 0; i < count; i++) defenseStatus.feedDogs();
            }
            case "pes" -> defenseStatus.addGuardDogs(count);
            case "plot" -> defenseStatus.addWoodenFence(count);
            case "elektricka_ohrada" -> defenseStatus.addElectricFence(count);
            case "turret" -> defenseStatus.addTurret(count);
            case "puska" -> defenseStatus.setHasRifle(true);
            case "brokovnice" -> defenseStatus.setHasShotgun(true);
        }
    }

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
                dogs, currentDogFoodDays, woodenFences, electricFences, turrets, hasRifle, hasShotgun
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

    // devtools, pouze pres konzoli
    private String handleDevCommand(String command, String param1, String param2) {
        return switch (command) {
            case "tp", "teleport" -> {
                if (param1 == null) yield "DEV: Zadej ID lokace (např. 'tp city_market', 'tp home').";
                Location targetLoc = gamePlan.getLocationById(param1);
                if (targetLoc == null) yield "DEV: Lokace s ID '" + param1 + "' neexistuje!";
                player.setCurrentLocation(targetLoc);
                yield "DEV: Teleportován do lokace " + targetLoc.getName() + " (" + targetLoc.getId() + ").";
            }
            case "give", "additem" -> {
                if (param1 == null) yield "DEV: Použití: give <id_itemu> [pocet]";
                int giveCount = parseCount(param2);
                if (itemCatalog.getItem(param1) == null) yield "DEV: Předmět '" + param1 + "' neexistuje v katalogu!";
                player.getInventory().addItem(param1, giveCount);
                yield "DEV: Přidáno " + giveCount + "x " + param1 + " do inventáře.";
            }
            case "addmoney", "money" -> {
                int moneyAmount = parseCount(param1);
                player.addMoney(moneyAmount);
                yield "DEV: Přidáno " + moneyAmount + " Kč. Aktuální stav: " + player.getMoney() + " Kč.";
            }
            case "addenergy", "energy" -> {
                int energyAmount = parseCount(param1);
                player.addEnergy(energyAmount);
                yield "DEV: Doplněno +" + energyAmount + " EN. Aktuální stav: " + player.getEnergy() + "/100 EN.";
            }
            case "setday" -> {
                if (param1 == null) yield "DEV: Zadej číslo dne (např. 'setday 10').";
                try {
                    int targetDay = Integer.parseInt(param1);
                    this.currentDay = Math.max(1, targetDay);
                    yield "DEV: Den nastaven na " + currentDay + ".";
                } catch (NumberFormatException e) {
                    yield "DEV: Neplatné číslo dne!";
                }
            }
            case "setweather" -> devSetWeather(param1);
            case "setvehicle" -> {
                if (param1 == null) yield "DEV: Použití: setvehicle <bike|car|van>";
                switch (param1) {
                    case "bike" -> player.setBestVehicle(TransportType.BIKE);
                    case "car" -> player.setBestVehicle(TransportType.CAR);
                    case "van" -> player.setBestVehicle(TransportType.VAN);
                    default -> { yield "DEV: Neznámé vozidlo (možnosti: bike, car, van)."; }
                }
                yield "DEV: Vozidlo změněno na " + player.getBestVehicle().getDisplayName() + ".";
            }
            case "growall", "dozraj" -> {
                for (farming.Plot plot : fieldManager.getPlots()) {
                    if (!plot.isEmpty() && !plot.isWithered()) {
                        while (!plot.isMature()) {
                            plot.water();
                            plot.advanceDay(false, false);
                        }
                    }
                }
                yield "DEV: Všechny plodiny na poli okamžitě dozrály!";
            }
            case "raid", "zombieraid" -> {
                syncDefenseStatusFromInventory();
                NightRaidResult raidResult = zombieManager.resolveNightRaid(
                        currentDay, defenseStatus, weatherManager.isFlooded()
                );
                yield "DEV: Testovací raid spuštěn!\n" + raidResult.getSummaryLog();
            }
            case "items", "predmety", "listitems" -> devListItems();
            case "devhelp", "dev", "cheats" -> getDevHelpText();
            default -> "DEV: Neznámý příkaz.";
        };
    }

    private String devSetWeather(String type) {
        if (type == null) return "DEV: Použití: setweather <slunecno|sucho|zaplava>";

        switch (type.toLowerCase()) {
            case "fair", "slunecno", "normal" -> {
                weatherManager.setCurrentWeather(WeatherType.NORMAL);
                return "DEV: Počasí ručně změněno na Slunečno.";
            }
            case "drought", "sucho" -> {
                weatherManager.setCurrentWeather(WeatherType.DROUGHT);
                return "DEV: Počasí ručně změněno na Sucho.";
            }
            case "flood", "zaplava" -> {
                weatherManager.setCurrentWeather(WeatherType.FLOOD);
                return "DEV: Počasí ručně změněno na Záplavu.";
            }
            default -> {
                return "DEV: Neznámý typ počasí! Možnosti: normal/slunecno, drought/sucho, flood/zaplava.";
            }
        }
    }

    private String devListItems() {
        List<Item> allItems = new ArrayList<>(itemCatalog.getCatalog().values());
        if (allItems.isEmpty()) {
            return "DEV: Katalog předmětů je prázdný.";
        }

        Collator czechCollator = Collator.getInstance(new Locale("cs", "CZ"));
        allItems.sort(Comparator
                .comparing(Item::getType)
                .thenComparing((a, b) -> czechCollator.compare(a.getName(), b.getName()))
        );

        StringBuilder sb = new StringBuilder("=== DEV: VŠECHNY PŘEDMĚTY V KATALOGU ===");
        ItemType lastType = null;

        for (Item item : allItems) {
            if (item.getType() != lastType) {
                lastType = item.getType();
                sb.append("\n\n--- ").append(lastType.getDisplayName().toUpperCase()).append(" ---");
            }
            sb.append(String.format("\n- ID: %-20s | %-22s | Cena: %d Kč",
                    item.getId(), item.getName(), item.getPrice()));
        }

        return sb.toString();
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

    private String getDevHelpText() {
        return "=== DEVTOOLS / CHEATY (Pouze pro CLI) ===\n" +
                "- tp / teleport <id_lokace>           : Teleport na lokaci (např. 'tp city_market', 'tp home')\n" +
                "- give / additem <id_itemu> [pocet]   : Přidá předmět do inventáře (např. 'give seminko_obili 10')\n" +
                "- money / addmoney <castka>          : Přidá peníze hráči (např. 'money 5000')\n" +
                "- energy / addenergy <castka>        : Doplní energii (např. 'energy 50')\n" +
                "- setday <cislo_dne>                 : Posune hru na konkrétní den (např. 'setday 10')\n" +
                "- setweather <slunecno|sucho|zaplava>: Změní počasí (normal, drought, flood)\n" +
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

    // gettery
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
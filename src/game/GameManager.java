package game;

import assets.GameContext;
import assets.NpcManager;
import assets.QuestEffect;
import farming.Crop;
import farming.CropCatalog;
import farming.FieldManager;
import farming.Plot;
import inventory.Item;
import inventory.ItemCatalog;
import inventory.ItemType;
import inventory.ShopManager;
import loader.NpcLoader;
import map.GamePlan;
import map.Location;
import map.TransportType;
import player.Player;
import save.GameState;
import save.GameStorage;
import save.JsonGameStorage;
import weather.WeatherManager;
import weather.WeatherType;
import zombies.DefenseStatus;
import zombies.NightRaidResult;
import zombies.ZombieManager;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    private final GameStorage storage; // kam se uklada hra (rozhrani - konkretni format nezname)

    private int currentDay = 1;
    private boolean isGameOver = false;

    // bezna hra: ukladani do slozky "saves" vedle spustene hry
    public GameManager() {
        this(new JsonGameStorage(Path.of("saves")));
    }

    // vlastni uloziste - hodi se hlavne v testech (docasna slozka)
    public GameManager(GameStorage storage) {
        this.storage = storage;
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
        this.defenseStatus = new DefenseStatus(itemCatalog);

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

        // KONTROLA GAME OVER: Zablokování hry po prohře/výhře
        if (isGameOver) {
            if (command.equals("konec") || command.equals("exit") || command.equals("quit")) {
                System.exit(0); // Okamžitě ukončí celou Java aplikaci (GUI i CLI)
                return "Hra se ukončuje...";
            }
            // po konci hry jde jeste nacist ulozenou hru a zkusit to znovu
            if (command.equals("nacti") || command.equals("load")) {
                return loadGame(param1);
            }
            if (command.equals("ulozene") || command.equals("saves")) {
                return listSavedGames();
            }
            return "Hra skončila. Můžeš načíst uloženou hru ('nacti [nazev]') nebo hru ukončit ('konec').";
        }

        return switch (command) {
            // Ukončení hry kdykoliv během hraní
            case "konec", "exit", "quit" -> {
                System.exit(0);
                yield "Hra se ukončuje...";
            }

            // Základní pohyb a stav
            case "jdi", "go", "move" -> (param1 == null) ? "Kam chceš jít? Použití: jdi <id_lokality>"
                    : gamePlan.movePlayer(player, param1);
            case "inventar", "inv" -> player.getInventory().getFormattedInventory();
            case "stav", "status" -> getPlayerStatus();
            case "napoveda", "pomoc", "help" -> getHelpText();
            case "spi", "sleep" -> handleSleep();
            case "jez", "eat" -> handleEat(param1);
            case "nakrm_psy" -> handleFeedDogs();

            // Ukladani a nacitani hry
            case "uloz", "save" -> saveGame(param1);
            case "nacti", "load" -> loadGame(param1);
            case "ulozene", "saves" -> listSavedGames();

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

    // =========================================================
    // UKLADANI A NACITANI HRY
    // =========================================================

    private static final String DEFAULT_SAVE_NAME = "hra";

    // uloz [nazev]: bez nazvu se uklada pod "hra"
    private String saveGame(String name) {
        String saveName = (name == null) ? DEFAULT_SAVE_NAME : name;
        try {
            storage.save(saveName, createSnapshot());
            return "Hra uložena jako '" + saveName + "' (den " + currentDay + ").";
        } catch (IOException e) {
            return "Hru se nepodařilo uložit: " + e.getMessage();
        }
    }

    // nacti [nazev]: nejdriv vse zkontroluje, teprve pak prepise aktualni hru
    private String loadGame(String name) {
        String saveName = (name == null) ? DEFAULT_SAVE_NAME : name;
        try {
            GameState state = storage.load(saveName);
            String error = validateSnapshot(state);
            if (error != null) {
                return "Uloženou hru nelze načíst: " + error;
            }
            applySnapshot(state);
            return "Načtena hra '" + saveName + "'.\n\n" + getPlayerStatus();
        } catch (IOException e) {
            return "Hru se nepodařilo načíst: " + e.getMessage();
        }
    }

    private String listSavedGames() {
        try {
            List<String> saves = storage.listSaves();
            if (saves.isEmpty()) {
                return "Zatím nemáš žádnou uloženou hru. Ulož ji příkazem 'uloz [nazev]'.";
            }
            return "Uložené hry: " + String.join(", ", saves);
        } catch (IOException e) {
            return "Seznam uložených her nelze načíst: " + e.getMessage();
        }
    }

    // prevede aktualni stav hry na GameState (jen cisla, texty a ID)
    public GameState createSnapshot() {
        // Stream API: kazdy zahonek -> PlotState
        List<GameState.PlotState> plotStates = fieldManager.getPlots().stream()
                .map(plot -> new GameState.PlotState(
                        plot.isEmpty() ? null : plot.getCrop().getId(),
                        plot.getGrowthDaysCurrent(),
                        plot.isWateredToday(),
                        plot.wasWateredYesterday(),
                        plot.isWithered()))
                .toList();

        GameState.WeatherState weather = new GameState.WeatherState(
                weatherManager.getCurrentWeather().name(),
                weatherManager.getDisasterChance(),
                weatherManager.getCooldownDays(),
                weatherManager.getWellLevel());

        return new GameState(
                GameState.CURRENT_VERSION,
                currentDay,
                player.getCurrentLocation().getId(),
                player.getEnergy(),
                player.getMoney(),
                player.getBestVehicle().name(),
                player.hasEngineTuning(),
                new ArrayList<>(player.getPlantedVegetables()),
                new HashMap<>(player.getInventory().getItems()),
                plotStates,
                fieldManager.getSprinklerCount(),
                weather,
                defenseStatus.getDogFoodDaysLeft(),
                npcManager.saveStates());
    }

    // kontrola ulozeneho souboru pred nactenim; vraci popis chyby, nebo null kdyz je vse v poradku
    // (soubor mohl nekdo rucne upravit, nebo je z jine verze hry)
    private String validateSnapshot(GameState s) {
        if (s.formatVersion() != GameState.CURRENT_VERSION) {
            return "nepodporovaná verze souboru (" + s.formatVersion() + ").";
        }
        if (s.day() < 1) return "neplatný den " + s.day() + ".";
        if (gamePlan.getLocationById(s.locationId()) == null) {
            return "neznámá lokalita '" + s.locationId() + "'.";
        }
        if (!isEnumValue(TransportType.class, s.vehicle())) return "neznámé vozidlo '" + s.vehicle() + "'.";
        if (s.weather() == null || !isEnumValue(WeatherType.class, s.weather().type())) {
            return "neplatné počasí.";
        }
        if (s.inventory() == null || s.plots() == null || s.plantedVegetables() == null || s.questStates() == null) {
            return "v souboru chybí část dat.";
        }
        if (s.plots().size() != fieldManager.getPlots().size()) {
            return "špatný počet záhonků (" + s.plots().size() + ").";
        }

        // Stream API: najdi predmety / plodiny, ktere hra nezna
        List<String> unknownItems = s.inventory().keySet().stream()
                .filter(id -> !itemCatalog.hasItem(id))
                .toList();
        if (!unknownItems.isEmpty()) return "neznámé předměty v inventáři: " + unknownItems + ".";

        boolean badCount = s.inventory().values().stream().anyMatch(count -> count == null || count <= 0);
        if (badCount) return "neplatný počet kusů v inventáři.";

        List<String> unknownCrops = s.plots().stream()
                .map(GameState.PlotState::cropId)
                .filter(id -> id != null && cropCatalog.getCrop(id) == null)
                .toList();
        if (!unknownCrops.isEmpty()) return "neznámé plodiny na poli: " + unknownCrops + ".";

        return null;
    }

    private static <E extends Enum<E>> boolean isEnumValue(Class<E> type, String name) {
        if (name == null) return false;
        try {
            Enum.valueOf(type, name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // prepise aktualni hru stavem ze souboru (volat az po validateSnapshot)
    private void applySnapshot(GameState s) {
        currentDay = s.day();
        isGameOver = false;

        player.setCurrentLocation(gamePlan.getLocationById(s.locationId()));
        player.restoreState(s.energy(), s.money(), TransportType.valueOf(s.vehicle()),
                s.engineTuning(), s.plantedVegetables());

        player.getInventory().clear();
        s.inventory().forEach((itemId, count) -> player.getInventory().addItem(itemId, count));

        List<Plot> plots = fieldManager.getPlots();
        for (int i = 0; i < plots.size(); i++) {
            GameState.PlotState ps = s.plots().get(i);
            Crop crop = (ps.cropId() == null) ? null : cropCatalog.getCrop(ps.cropId());
            plots.get(i).restore(crop, ps.growthDays(), ps.wateredToday(), ps.wateredYesterday(), ps.withered());
        }
        fieldManager.restoreSprinklerCount(s.sprinklerCount());

        GameState.WeatherState w = s.weather();
        weatherManager.restoreState(WeatherType.valueOf(w.type()), w.disasterChance(), w.cooldownDays(), w.wellLevel());

        syncDefenseStatusFromInventory();
        defenseStatus.restoreDogFoodDays(s.dogFoodDaysLeft());

        npcManager.loadStates(s.questStates());
    }

    // jez <jidlo>: snedeni jidla z inventare doplni energii o "value" z items.json
    private String handleEat(String itemId) {
        if (itemId == null) return "Co chceš sníst? Použití: jez <jidlo>";

        Item item = itemCatalog.getItem(itemId);

        if (item == null || item.getType() != ItemType.FOOD) {
            return "'" + itemId + "' se nedá sníst.";
        }

        if (!player.getInventory().hasItem(itemId, 1)) {
            return "Tento předmět nemáš v inventáři.";
        }

        if (player.getEnergy() >= 100) {
            return "Nemáš hlad, energii máš plnou (100/100).";
        }

        // Načítá přesnou hodnotu doplnění energie přímo z items.json (atribut value)
        int dodanaEnergie = item.getValue();

        player.getInventory().removeItem(itemId, 1);
        int before = player.getEnergy();
        player.addEnergy(dodanaEnergie);

        return "Snědl jsi " + item.getName() + ". Energie +" + (player.getEnergy() - before)
                + " (" + player.getEnergy() + "/100).";
    }

    // nakrm_psy: spotrebuje 1x krmeni a psi brani farmu dalsich 5 dni
    private String handleFeedDogs() {
        if (!player.getCurrentLocation().getId().equalsIgnoreCase("field")) {
            return "Psy můžeš nakrmit pouze na poli!";
        }
        syncDefenseStatusFromInventory();
        if (defenseStatus.getGuardDogs() == 0) {
            return "Nemáš žádné hlídací psy. Kup si je na Místním trhu.";
        }
        // plne nasyceni psi: krmeni by se spotrebovalo zbytecne (sytost se nescita)
        if (defenseStatus.getDogFoodDaysLeft() >= DefenseStatus.DOG_FOOD_DAYS) {
            return "Psi jsou ještě sytí na " + DefenseStatus.DOG_FOOD_DAYS + " dní, krmení si nech na později.";
        }
        if (!player.getInventory().removeItem("krmeni_psy", 1)) {
            return "Nemáš krmení pro psy. Koupíš ho na Místním trhu.";
        }
        defenseStatus.feedDogs();
        return "Nakrmil jsi psy (" + defenseStatus.getGuardDogs() + "). Budou hlídat dalších "
                + DefenseStatus.DOG_FOOD_DAYS + " dní.";
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
            case "obstarej", "zali", "water" -> fieldManager.tendPlots(player, weatherManager.isDrought());
            case "sklid", "harvest" -> fieldManager.harvestAll(player, itemCatalog);
            case "pole", "zahony", "fieldstatus" -> fieldManager.getFieldStatus(itemCatalog, weatherManager.isDrought());
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

    // =========================================================
    // LOGIKA PROCHÁZENÍ DNÍ & VEDLEJŠÍ EFEKTY NÁKUPU
    // =========================================================

    public String advanceToNextDay() {
        StringBuilder sb = new StringBuilder();

        syncDefenseStatusFromInventory();

        NightRaidResult raidResult = zombieManager.resolveNightRaid(
                currentDay,
                defenseStatus,
                weatherManager.isFlooded()
        );

        // Zde řešíme situaci, kdy obrana padla
        if (raidResult.isAttackOccurred() && !raidResult.isDefenseSuccessful()) {
            sb.append(raidResult.getSummaryLog()).append("\n\n");
            sb.append("Z důvodu prolomení obrany se hráč v noci nemohl vyspat a NEDOPLNIL si energii!\n");

            // NOVÉ: Zombíci zničí všechny plodiny na poli
            boolean destroyedSomething = false;
            for (Plot plot : fieldManager.getPlots()) {
                if (!plot.isEmpty()) {
                    plot.clear(); // Vyčistí záhonek (stejně jako při vytrhání uschlé plodiny)
                    destroyedSomething = true;
                }
            }

            if (destroyedSomething) {
                sb.append("Zombíci navíc pošlapali a ZNIČILI veškerou úrodu na záhoncích!\n");
            }

        } else {
            // Obrana vydržela nebo útok nebyl
            if (raidResult.isAttackOccurred()) {
                sb.append(raidResult.getSummaryLog()).append("\n\n");
            }
            player.addEnergy(50);
            sb.append("Doplněno +50 energie ze spánku.\n");
        }

        // psum ubyde jeden den krmeni (kazdou noc, i kdyz zombici neprisli)
        defenseStatus.advanceDay();

        currentDay++;

        fieldManager.advanceDay(weatherManager.isDrought());
        sb.append("Plodiny na poli pokročily v růstu.\n\n");

        String weatherReport = weatherManager.advanceWeather(currentDay);
        sb.append("=== PROBUDIL SES DO DNE ").append(currentDay).append(" ===\n");
        sb.append(weatherReport);

        // Zkontroluje, zda hráč po zničení úrody nezkrachoval
        checkBankruptcy(sb);

        return sb.toString().trim();
    }

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
        }
    }

    // prepocita obranu podle inventare (psi, ploty, zbrane...); krmeni psu zustava
    private void syncDefenseStatusFromInventory() {
        defenseStatus.updateFromInventory(player.getInventory().getItems());
    }

    public String getPlayerStatus() {
        return String.format(
                "=== STAV HRÁČE ===\n" +
                        "Den: %d | Lokalita: %s (ID: %s)\n" +
                        "Počasí: %s\n" +
                        "Energie: %d/100 EN | Peníze: %d Kč\n" +
                        "Vozidlo: %s | Motorový tuning: %s\n" +
                        "Zasazeno unikátní zeleniny: %d/8\n" +
                        "%s",
                currentDay,
                player.getCurrentLocation().getName(),
                player.getCurrentLocation().getId(),
                weatherManager.getCurrentWeather().getDescription(),
                player.getEnergy(),
                player.getMoney(),
                player.getBestVehicle().getDisplayName(),
                player.hasEngineTuning() ? "ANO" : "NE",
                player.getPlantedVegetablesCount(),
                getDefenseSummary()
        );
    }

    // jeden radek o obrane farmy pro prikaz "stav"
    private String getDefenseSummary() {
        syncDefenseStatusFromInventory();
        String dogs = defenseStatus.getGuardDogs() == 0 ? "žádní"
                : defenseStatus.getGuardDogs() + " (" + (defenseStatus.areDogsFed()
                ? "nakrmení ještě " + defenseStatus.getDogFoodDaysLeft() + " dní" : "HLADOVÍ") + ")";
        return "Obrana: psi " + dogs
                + " | ploty " + defenseStatus.getWoodenFences()
                + " | el. ohrady " + defenseStatus.getElectricFences()
                + " | turrety " + defenseStatus.getTurrets();
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
                            plot.advanceDay(false);
                            plot.water();
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
            sb.append(String.format("\nID: %-20s | %-22s | Cena: %d Kč"
                    ,
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
                "- jez <jidlo>           : Sní jídlo z inventáře a doplní energii (např. 'jez chleb')\n" +
                "- nakrm_psy             : Nakrmí psy na 5 dní (pouze na poli, spotřebuje krmení)\n" +
                "- uloz [nazev]          : Uloží rozehranou hru (bez názvu jako 'hra')\n" +
                "- nacti [nazev]         : Načte uloženou hru\n" +
                "- ulozene               : Vypíše seznam uložených her\n" +
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

    private void checkBankruptcy(StringBuilder sb) {
        // 1. Zjistíme, jestli něco roste na poli (pokud ano, hráč ještě může něco sklidit)
        boolean hasCropsOnField = false;
        for (Plot plot : fieldManager.getPlots()) {
            if (!plot.isEmpty() && !plot.isWithered()) {
                hasCropsOnField = true;
                break;
            }
        }

        // 2. Zjistíme, jestli má hráč nějaké předměty k prodeji nebo zasazení
        boolean hasItemsInInventory = !player.getInventory().getItems().isEmpty();

        // 3. Zjistíme, jestli má hráč peníze alespoň na nejlevnější semínko
        //    (cena se bere z items.json, aby sedela i po zmene cen)
        int minSeedPrice = itemCatalog.getCatalog().values().stream()
                .filter(item -> item.getType() == ItemType.SEED)
                .mapToInt(Item::getPrice)
                .min()
                .orElse(0);

        // 4. Pokud nemá plodiny, nemá předměty a nemá peníze = PROHRA
        if (!hasCropsOnField && !hasItemsInInventory && player.getMoney() < minSeedPrice) {
            isGameOver = true;
            sb.append("\n====================================================\n");
            sb.append("*** GAME OVER ***\n");
            sb.append("Zombíci ti zničili úrodu. Nemáš peníze na obnovení farmy, \n");
            sb.append("nemáš nic k prodeji a pole je prázdné.\n");
            sb.append("Tvá farma zkrachovala...\n");
            sb.append("====================================================\n");
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
package gui;

import assets.Npc;
import game.GameManager;
import inventory.Item;
import inventory.ItemType;
import map.Location;
import map.Passage;
import player.Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameWindow extends JFrame {

    private final GameManager gameManager;

    private JLabel statusLabel;
    private JTextArea logTextArea;

    private CardLayout bottomCardLayout;
    private JPanel bottomCardPanel;
    private JPanel navigationPanel;
    private JPanel actionPanel;
    private JPanel savePanel;

    private JTextField cliInputField;
    private JButton toggleModeButton;
    private boolean isGuiMode = true;

    public GameWindow(GameManager gameManager) {
        super("Farming Game - Post-apokalyptická farma");
        this.gameManager = gameManager;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // nahore status bar a prepinac CLI/GUI
        add(createTopPanel(), BorderLayout.NORTH);

        // uprostred log
        add(createCenterLogPanel(), BorderLayout.CENTER);

        // spodni karta
        add(createBottomCardPanel(), BorderLayout.SOUTH);

        updateUIState();
        appendLog("Vítej ve hře! Pro zobrazení seznamu příkazů v CLI napiš 'napoveda'.");
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        statusLabel = new JLabel("Načítání stavu...");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        topPanel.add(statusLabel, BorderLayout.CENTER);

        toggleModeButton = new JButton("Režim: Tlačítka");
        toggleModeButton.addActionListener(e -> toggleInterfaceMode());
        topPanel.add(toggleModeButton, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createCenterLogPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Log"));

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        return centerPanel;
    }

    private JPanel createBottomCardPanel() {
        bottomCardLayout = new CardLayout();
        bottomCardPanel = new JPanel(bottomCardLayout);

        JPanel guiCard = new JPanel(new GridLayout(3, 1, 5, 5));
        guiCard.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        navigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navigationPanel.setBorder(BorderFactory.createTitledBorder("Pohyb po mapě"));

        actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Dostupné akce"));

        savePanel = createSavePanel();

        guiCard.add(navigationPanel);
        guiCard.add(actionPanel);
        guiCard.add(savePanel);

        JPanel cliCard = new JPanel(new BorderLayout(5, 5));
        cliCard.setBorder(BorderFactory.createTitledBorder("Konzolový vstup"));

        cliInputField = new JTextField();
        cliInputField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        cliInputField.addActionListener(e -> handleCliSubmit());

        JButton sendButton = new JButton("Odeslat příkaz");
        sendButton.addActionListener(e -> handleCliSubmit());

        cliCard.add(cliInputField, BorderLayout.CENTER);
        cliCard.add(sendButton, BorderLayout.EAST);

        bottomCardPanel.add(guiCard, "GUI_CARD");
        bottomCardPanel.add(cliCard, "CLI_CARD");

        return bottomCardPanel;
    }

    // samostatna lista pro ukladani a nacitani hry pod listou akci
    private JPanel createSavePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Ukládání a načítání hry"));

        JButton listSavesBtn = new JButton("Seznam uložení");
        listSavesBtn.addActionListener(e -> {
            String result = gameManager.processCommand("ulozene");
            appendLog(result);
            updateUIState();
        });
        panel.add(listSavesBtn);

        JButton saveBtn = new JButton("Uložit hru");
        saveBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(
                    this,
                    "Zadej název uložení (nebo nech prázdné pro výchozí 'hra'):",
                    "Uložit hru",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (name != null) {
                String cmd = name.trim().isEmpty() ? "uloz" : "uloz " + name.trim();
                String result = gameManager.processCommand(cmd);
                appendLog(result);
                updateUIState();
            }
        });
        panel.add(saveBtn);

        JButton loadBtn = new JButton("Načíst hru");
        loadBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(
                    this,
                    "Zadej název pozice k načtení (nebo nech prázdné pro výchozí 'hra'):",
                    "Načíst hru",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (name != null) {
                String cmd = name.trim().isEmpty() ? "nacti" : "nacti " + name.trim();
                String result = gameManager.processCommand(cmd);
                appendLog(result);
                updateUIState();
            }
        });
        panel.add(loadBtn);

        return panel;
    }

    private void toggleInterfaceMode() {
        isGuiMode = !isGuiMode;
        if (isGuiMode) {
            bottomCardLayout.show(bottomCardPanel, "GUI_CARD");
            toggleModeButton.setText("Režim: Tlačítka");
        } else {
            bottomCardLayout.show(bottomCardPanel, "CLI_CARD");
            toggleModeButton.setText("Režim: Konzole");
            cliInputField.requestFocusInWindow();
        }
    }

    private void handleCliSubmit() {
        String input = cliInputField.getText().trim();
        if (!input.isEmpty()) {
            appendLog("> " + input);
            String result = gameManager.processCommand(input);
            appendLog(result);
            cliInputField.setText("");
            updateUIState();
        }
    }

    public void updateUIState() {
        updateStatusBar();
        refreshActionButtons();
        refreshNavigationButtons();
    }

    private void updateStatusBar() {
        Player player = gameManager.getPlayer();
        statusLabel.setText(String.format(
                "Den: %d | Lokace: %s | Počasí: %s | Energie: %d/100 | Peníze: %d Kč | Vozidlo: %s",
                gameManager.getCurrentDay(),
                player.getCurrentLocation().getName(),
                gameManager.getWeatherManager().getCurrentWeather().getDescription(),
                player.getEnergy(),
                player.getMoney(),
                player.getBestVehicle().getDisplayName()
        ));
    }

    private void refreshActionButtons() {
        actionPanel.removeAll();
        String locId = gameManager.getPlayer().getCurrentLocation().getId().toLowerCase();

        // Zakladni tlacitka dostupna vsude
        addActionButton("Stav hráče", "stav");
        addActionButton("Inventář", "inventar");

        JButton eatBtn = new JButton("Sníst jídlo...");
        eatBtn.addActionListener(e -> openEatDialog());
        actionPanel.add(eatBtn);

        // Tlacitka podle lokace
        switch (locId) {
            case "field":
                JButton plantBtn = new JButton("Zasadit...");
                plantBtn.addActionListener(e -> openPlantDialog());
                actionPanel.add(plantBtn);

                addActionButton("Obstarat záhonky", "obstarej");
                addActionButton("Sklidit úrodu", "sklid");
                addActionButton("Stav záhonků", "pole");
                addActionButton("Nakrmit psy", "nakrm_psy");
                break;

            case "home":
                addActionButton("Vyspat se (Nový den)", "spi");
                break;

            case "local_market":
            case "city_market":
            case "autobazar":
            case "army_shop":
                JButton shopBtn = new JButton("Otevřít Obchod");
                shopBtn.addActionListener(e -> openShopDialog());
                actionPanel.add(shopBtn);
                break;
        }

        // tlacitko pro npc
        Map<String, assets.Npc> localNpcs = gameManager.getNpcManager().getNpcsInLocation(locId);
        if (!localNpcs.isEmpty()) {
            JButton npcBtn = new JButton("Postavy (NPC)");
            npcBtn.addActionListener(e -> openNpcDialog(localNpcs));
            actionPanel.add(npcBtn);
        }

        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void refreshNavigationButtons() {
        navigationPanel.removeAll();
        Location currentLoc = gameManager.getPlayer().getCurrentLocation();
        Map<String, Passage> passages = currentLoc.getPassages();

        for (Map.Entry<String, Passage> entry : passages.entrySet()) {
            String targetId = entry.getKey();
            String targetName = entry.getValue().getDestination().getName();

            JButton moveButton = new JButton("Jdi: " + targetName);
            moveButton.addActionListener(e -> {
                String result = gameManager.processCommand("jdi " + targetId);
                appendLog(result);
                updateUIState();
            });
            navigationPanel.add(moveButton);
        }

        navigationPanel.revalidate();
        navigationPanel.repaint();
    }

    private void addActionButton(String label, String command) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            String result = gameManager.processCommand(command);
            appendLog(result);
            updateUIState();
        });
        actionPanel.add(btn);
    }

    // vyber jidla z inventare pres dropdown
    private void openEatDialog() {
        Player player = gameManager.getPlayer();
        Map<String, Integer> inventoryItems = player.getInventory().getItems();
        List<FoodOption> availableFood = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : inventoryItems.entrySet()) {
            Item item = gameManager.getItemCatalog().getItem(entry.getKey());
            if (item != null && item.getType() == ItemType.FOOD && entry.getValue() > 0) {
                availableFood.add(new FoodOption(item, entry.getValue()));
            }
        }

        if (availableFood.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nemáš v batohu žádné jídlo!",
                    "Prázdný inventář",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        FoodOption selected = (FoodOption) JOptionPane.showInputDialog(
                this,
                "Vyber jídlo z inventáře, které chceš sníst:",
                "Sníst jídlo",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableFood.toArray(),
                availableFood.get(0)
        );

        if (selected != null) {
            String command = "jez " + selected.getItem().getId();
            String result = gameManager.processCommand(command);
            appendLog(result);
            updateUIState();
        }
    }

    // vyber seminka pri sazeni
    private void openPlantDialog() {
        Player player = gameManager.getPlayer();
        Map<String, Integer> inventoryItems = player.getInventory().getItems();
        List<SeedOption> availableSeeds = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : inventoryItems.entrySet()) {
            Item item = gameManager.getItemCatalog().getItem(entry.getKey());
            if (item != null && item.getType() == ItemType.SEED && entry.getValue() > 0) {
                availableSeeds.add(new SeedOption(item, entry.getValue()));
            }
        }

        if (availableSeeds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nemáš v batohu žádná semínka k zasazení!", "Prázdný inventář", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        SeedOption selected = (SeedOption) JOptionPane.showInputDialog(
                this,
                "Vyber semínko z inventáře k zasazení:",
                "Zasadit plodinu",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableSeeds.toArray(),
                availableSeeds.get(0)
        );

        if (selected != null) {
            String command = "zasad " + selected.getItem().getId();
            String result = gameManager.processCommand(command);
            appendLog(result);
            updateUIState();
        }
    }

    private void openShopDialog() {
        ShopDialog dialog = new ShopDialog(this, gameManager);
        dialog.setVisible(true);
    }

    public void appendLog(String message) {
        if (message != null && !message.trim().isEmpty()) {
            logTextArea.append(message + "\n----------------------------------------\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
    }

    // pomocna trida pro vyber jidla v dropdownu
    private static class FoodOption {
        private final Item item;
        private final int count;

        public FoodOption(Item item, int count) {
            this.item = item;
            this.count = count;
        }

        public Item getItem() {
            return item;
        }

        @Override
        public String toString() {
            return String.format("%s (+%d EN, %d ks)", item.getName(), item.getValue(), count);
        }
    }

    // pomocna trida pro vyber seminka pri sazeni
    private static class SeedOption {
        private final Item item;
        private final int count;

        public SeedOption(Item item, int count) {
            this.item = item;
            this.count = count;
        }

        public Item getItem() {
            return item;
        }

        @Override
        public String toString() {
            return String.format("%s (%d ks)", item.getName(), count);
        }
    }

    // interakce s postavami v aktualni lokaci
    private void openNpcDialog(Map<String, Npc> localNpcs) {
        if (localNpcs.isEmpty()) return;

        String selectedNpcId;
        Npc selectedNpc;

        // pokud je v lokaci 1 postava, automaticky ji vybereme
        // pokud jich je vice, dame seznam
        if (localNpcs.size() == 1) {
            Map.Entry<String, Npc> entry = localNpcs.entrySet().iterator().next();
            selectedNpcId = entry.getKey();
            selectedNpc = entry.getValue();
        } else {
            List<NpcOption> options = new ArrayList<>();
            for (Map.Entry<String, Npc> entry : localNpcs.entrySet()) {
                options.add(new NpcOption(entry.getKey(), entry.getValue().getName()));
            }
            NpcOption chosen = (NpcOption) JOptionPane.showInputDialog(
                    this,
                    "Vyber postavu, se kterou chceš interagovat:",
                    "Postavy v lokaci",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options.toArray(),
                    options.get(0)
            );
            if (chosen == null) return; // storno nebo zavreni dialogu
            selectedNpcId = chosen.getId();
            selectedNpc = localNpcs.get(selectedNpcId);
        }

        // volba akce pro dane npc
        Object[] actionButtons = {"Mluvit", "Odevzdat úkol", "Zrušit"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Co chceš s postavou " + selectedNpc.getName() + " udělat?",
                "Interakce: " + selectedNpc.getName(),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                actionButtons,
                actionButtons[0]
        );

        if (choice == 0) { // mluvit
            String result = gameManager.processCommand("mluv " + selectedNpcId);
            appendLog(result);
            updateUIState();
        } else if (choice == 1) { // odevzdat ukol
            String result = gameManager.processCommand("ukol " + selectedNpcId);
            appendLog(result);
            updateUIState();
        }
    }

    // pomocna trida pro vyber npc
    private static class NpcOption {
        private final String id;
        private final String name;

        public NpcOption(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
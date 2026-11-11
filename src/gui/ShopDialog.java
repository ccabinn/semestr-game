package gui;

import game.GameManager;
import inventory.Item;
import inventory.ItemCatalog;
import inventory.ItemType;
import player.Player;

import javax.swing.*;

import java.awt.*;

import java.text.Collator;

import java.util.*;
import java.util.List;

public class ShopDialog extends JDialog {

    private final GameManager gameManager;
    private final GameWindow parentWindow;

    private DefaultListModel<ItemWrapper> buyListModel;
    private JList<ItemWrapper> buyList;
    private JSpinner buySpinner;

    private DefaultListModel<InventoryItemWrapper> sellListModel;
    private JList<InventoryItemWrapper> sellList;
    private JSpinner sellSpinner;

    public ShopDialog(GameWindow parentWindow, GameManager gameManager) {
        super(parentWindow, "Obchod - " + gameManager.getPlayer().getCurrentLocation().getName(), true);
        this.parentWindow = parentWindow;
        this.gameManager = gameManager;

        setSize(750, 480);
        setLocationRelativeTo(parentWindow);
        setLayout(new GridLayout(1, 2, 10, 10));

        add(createBuyPanel());
        add(createSellPanel());

        refreshData();
    }

    private JPanel createBuyPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Nákup zboží"));

        buyListModel = new DefaultListModel<>();
        buyList = new JList<>(buyListModel);
        buyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(buyList), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Počet:"));
        buySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        bottomPanel.add(buySpinner);

        JButton buyButton = new JButton("Koupit");
        buyButton.addActionListener(e -> executeBuy());
        bottomPanel.add(buyButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSellPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Prodej z inventáře"));

        sellListModel = new DefaultListModel<>();
        sellList = new JList<>(sellListModel);
        sellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(sellList), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Počet:"));
        sellSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        bottomPanel.add(sellSpinner);

        JButton sellButton = new JButton("Prodat");
        sellButton.addActionListener(e -> executeSell());
        bottomPanel.add(sellButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshData() {
        refreshBuyList();
        refreshSellList();
    }

    private void refreshBuyList() {
        buyListModel.clear();
        Player player = gameManager.getPlayer();
        String currentLocationId = player.getCurrentLocation().getId().toLowerCase();

        // Použití tvojí metody z ItemCatalog
        List<Item> availableItems = new ArrayList<>(gameManager.getItemCatalog().getItemsForLocation(currentLocationId));

        // Seřazení podle kategorií a následně abecedně česky
        Collator collator = Collator.getInstance(new Locale("cs", "CZ"));
        availableItems.sort((a, b) -> {
            int catCompare = Integer.compare(getCategoryOrder(a.getType()), getCategoryOrder(b.getType()));
            if (catCompare != 0) return catCompare;
            return collator.compare(a.getName(), b.getName());
        });

        for (Item item : availableItems) {
            buyListModel.addElement(new ItemWrapper(item));
        }
    }

    private void refreshSellList() {
        sellListModel.clear();
        Player player = gameManager.getPlayer();
        String currentLocationId = player.getCurrentLocation().getId().toLowerCase();
        Map<String, Integer> inventoryItems = player.getInventory().getItems();

        for (Map.Entry<String, Integer> entry : inventoryItems.entrySet()) {
            Item item = gameManager.getItemCatalog().getItem(entry.getKey());
            if (item != null) {
                int count = entry.getValue();
                sellListModel.addElement(new InventoryItemWrapper(item, count, currentLocationId));
            }
        }
    }

    private int getCategoryOrder(ItemType type) {
        switch (type) {
            case SEED: return 1;
            case FOOD: return 2;
            case WEAPON: return 3;
            case DEFENSE_PASSIVE: return 4;
            case DOG_FOOD: return 5;
            case UPGRADE: return 6;
            case VEHICLE: return 7;
            default: return 8;
        }
    }

    private void executeBuy() {
        ItemWrapper selected = buyList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vyber předmět k nákupu!", "Upozornění", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int count = (int) buySpinner.getValue();
        String command = "kup " + selected.getItem().getId() + " " + count;
        String result = gameManager.processCommand(command);

        parentWindow.appendLog(result);
        parentWindow.updateUIState();
        refreshData();
    }

    private void executeSell() {
        InventoryItemWrapper selected = sellList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vyber předmět k prodeji!", "Upozornění", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int count = (int) sellSpinner.getValue();
        String command = "prodej " + selected.getItem().getId() + " " + count;
        String result = gameManager.processCommand(command);

        parentWindow.appendLog(result);
        parentWindow.updateUIState();
        refreshData();
    }

    // Pomocná obalová třída pro formátování v JListu (Nákup)
    private static class ItemWrapper {
        private final Item item;

        public ItemWrapper(Item item) { this.item = item; }
        public Item getItem() { return item; }

        @Override
        public String toString() {
            return String.format("[%s] %s — %d Kč", getCategoryLabel(item.getType()), item.getName(), item.getPrice());
        }

        private String getCategoryLabel(ItemType type) {
            switch (type) {
                case SEED: return "SEMÍNKO";
                case FOOD: return "JÍDLO";
                case WEAPON: return "ZBRAŇ";
                case DEFENSE_PASSIVE: return "PASIVNÍ OBRANA";
                case DOG_FOOD: return "KRMENÍ";
                case UPGRADE: return "VYLEPŠENÍ";
                case VEHICLE: return "DOPRAVNÍ PROSTŘEDEK";
                default: return "OSTATNÍ";
            }
        }
    }

    // Pomocná obalová třída pro formátování v JListu (Prodej)
    private static class InventoryItemWrapper {
        private final Item item;
        private final int count;
        private final String locationId;

        public InventoryItemWrapper(Item item, int count, String locationId) {
            this.item = item;
            this.count = count;
            this.locationId = locationId;
        }

        public Item getItem() { return item; }

        @Override
        public String toString() {
            int unitSellPrice;
            if (item.getType() == ItemType.CROP) {
                unitSellPrice = locationId.equals("city_market") ? item.getPrice() * 2 : item.getPrice();
            } else {
                unitSellPrice = (int) Math.round(item.getPrice() * 0.8);
            }
            return String.format("%s (%d ks) — Výkup: %d Kč/ks", item.getName(), count, unitSellPrice);
        }
    }
}
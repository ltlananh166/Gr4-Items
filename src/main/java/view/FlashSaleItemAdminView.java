package view;

import model.FlashSaleItem;
import repository.FlashSaleItemRepository;

import java.util.List;
import java.util.Objects;

public class FlashSaleItemAdminView {
    private static final int DISPLAY_LIMIT = 30;

    private final FlashSaleItemRepository items;
    private final ConsoleInput input;

    public FlashSaleItemAdminView(FlashSaleItemRepository items, ConsoleInput input) {
        this.items = Objects.requireNonNull(items, "items");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== FLASH SALE ITEM MANAGEMENT =====");
            System.out.println("1. List items");
            System.out.println("2. Search by item/product/event ID");
            System.out.println("3. Reset item stock to a new allocation");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Choose an option", 0, 3,
                    "Choose a number from 0 to 3.");
            switch (choice) {
                case 1 -> displayItems(items.findAll(), "Flash Sale items");
                case 2 -> searchItems();
                case 3 -> resetStock();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated item menu choice was out of range.");
            }
        }
    }

    private void searchItems() {
        String query = input.readStringRequired("Item, product, or event ID: ");
        displayItems(items.findAll().stream()
                .filter(item -> query.equalsIgnoreCase(item.getId())
                        || query.equalsIgnoreCase(item.getProductId())
                        || query.equalsIgnoreCase(item.getEventId()))
                .toList(), "Search results");
    }

    private void resetStock() {
        String itemId = input.readStringRequired("Flash Sale item ID: ");
        FlashSaleItem item = items.findById(itemId);
        if (item == null) {
            System.out.println("Item not found.");
            return;
        }
        int quantity = input.readIntMin("New stock allocation: ", 0,
                "Stock cannot be negative.");
        items.resetStock(itemId, quantity);
        items.persist();
        System.out.println("Stock allocation saved for " + itemId + ".");
    }

    private void displayItems(List<FlashSaleItem> values, String title) {
        System.out.printf("%s: %d%n", title, values.size());
        System.out.printf("%-10s %-10s %-10s %-28s %12s %12s %8s%n",
                "Item ID", "Product", "Event", "Name", "Price", "Initial", "Remain");
        for (FlashSaleItem item : values.stream().limit(DISPLAY_LIMIT).toList()) {
            System.out.printf("%-10s %-10s %-10s %-28s %,12.0f %,12d %8d%n",
                    item.getId(), item.getProductId(), item.getEventId(),
                    trim(item.getProductName(), 28), item.getSalePrice().doubleValue(),
                    item.getInitialStock(), item.getRemainingStock());
        }
        if (values.size() > DISPLAY_LIMIT) {
            System.out.printf("Showing first %d; search by ID for a specific item.%n",
                    DISPLAY_LIMIT);
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 3) + "...";
    }
}

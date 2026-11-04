package view;

import controller.FlashSaleController;
import controller.ProductController;
import model.FlashSaleItem;
import model.RegularProduct;

import java.math.BigDecimal;
import java.util.List;

/** Giao diện CRUD mặt hàng Flash Sale, chỉ sử dụng các API của bộ điều khiển. */
public class FlashSaleItemAdminView {
    private final FlashSaleController flashSaleController;
    private final ProductController productController;
    private final FlashSaleView tableView;
    private final ConsoleInput input;

    public FlashSaleItemAdminView(FlashSaleController flashSaleController,
                                  ProductController productController,
                                  FlashSaleView tableView,
                                  ConsoleInput input) {
        this.flashSaleController = flashSaleController;
        this.productController = productController;
        this.tableView = tableView;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> listByEvent();
                    case 2 -> createItem();
                    case 3 -> updateItem();
                    case 4 -> deleteItem();
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Flash Sale item operation cancelled.");
            } catch (Exception exception) {
                System.out.println("Unable to process Flash Sale item: " + exception.getMessage());
            }
        }
    }

    public void printMenu() {
        System.out.println("\n--- FLASH SALE ITEM MANAGEMENT ---");
        System.out.println("1. List items by event");
        System.out.println("2. Add a product to an event");
        System.out.println("3. Update price/stock limit");
        System.out.println("4. Delete item");
        System.out.println("0. Back");
    }

    public void listByEvent() {
        String eventId = input.readStringRequired("Event ID");
        tableView.displayItems(flashSaleController.getItemsByEvent(eventId));
    }

    public void createItem() throws Exception {
        String itemId = input.readStringRequired("Flash Sale item ID");
        String eventId = input.readStringRequired("Event ID");
        flashSaleController.findEvent(eventId);
        String productId = input.readStringRequired("Product ID");
        RegularProduct product = productController.findProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        int salePrice = input.readIntMin("Flash Sale price", 0, "Price cannot be negative");
        int limitedStock = input.readIntMin("Stock limit", 0, "Stock cannot be negative");
        FlashSaleItem item = new FlashSaleItem(itemId, eventId, productId,
                product.getPrice(), BigDecimal.valueOf(salePrice),
                limitedStock, 0, 2, 0);
        item.setProductName(product.getName());
        flashSaleController.addFlashSaleItem(item);
        System.out.println("Flash Sale item added.");
    }

    public void updateItem() throws Exception {
        String itemId = input.readStringRequired("Item ID to update");
        FlashSaleItem item = flashSaleController.findFlashSaleItem(itemId);
        if (item == null) {
            System.out.println("Flash Sale item not found.");
            return;
        }
        int expectedVersion = item.getVersion();
        int salePrice = readIntOrDefault("Sale price",
                item.getSalePrice().setScale(0, java.math.RoundingMode.HALF_UP).intValueExact(), 0);
        int limitedStock = readIntOrDefault("Stock limit", item.getInitialStock(), 0);
        item.setSalePrice(BigDecimal.valueOf(salePrice));
        item.setQuantity(limitedStock);
        if (flashSaleController.updateFlashSaleItem(item, expectedVersion)) {
            System.out.println("Item updated. New version: " + item.getVersion());
        } else {
            System.out.println("The version changed in another session. Reload and try again.");
        }
    }

    public void deleteItem() {
        String itemId = input.readStringRequired("Item ID to delete");
        if (!"DELETE".equalsIgnoreCase(input.readLine("Enter DELETE to confirm: "))) {
            System.out.println("Deletion cancelled.");
            return;
        }
        System.out.println(flashSaleController.deleteFlashSaleItem(itemId)
                ? "Item deleted." : "Item not found.");
    }

    public int readIntOrDefault(String label, int current, int minimum) {
        String raw = input.readLine(label + " [" + current + "]: ");
        if (raw.isBlank()) {
            return current;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value < minimum) {
                throw new IllegalArgumentException(label + " must be at least " + minimum);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be an integer", exception);
        }
    }
}

// Thành viên 3

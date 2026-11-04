package view;

import controller.ProductController;
import model.RegularProduct;

import java.math.BigDecimal;
import java.util.List;

/** CRUD sản phẩm và tìm kiếm theo danh mục/giá thông qua ProductController. */
public class ProductAdminView {
    private static final int DISPLAY_LIMIT = 20;

    private final ProductController controller;
    private final ConsoleInput input;

    public ProductAdminView(ProductController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> displayProducts(controller.listProducts());
                    case 2 -> createProduct();
                    case 3 -> updateProduct();
                    case 4 -> deleteProduct();
                    case 5 -> searchProducts();
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Product operation cancelled.");
            } catch (IllegalArgumentException exception) {
                System.out.println("Unable to process product: " + exception.getMessage());
            }
        }
    }

    public void printMenu() {
        System.out.println("\n--- PRODUCT MANAGEMENT ---");
        System.out.println("1. List products");
        System.out.println("2. Add product");
        System.out.println("3. Update product");
        System.out.println("4. Delete product");
        System.out.println("5. Search by category and price range");
        System.out.println("0. Back");
    }

    public void createProduct() {
        String id = input.readStringRequired("Product ID");
        RegularProduct product = readProduct(id, null);
        controller.createProduct(product);
        System.out.println("Product added: " + id + ".");
    }

    public void updateProduct() {
        String id = input.readStringRequired("Product ID to update");
        RegularProduct current = controller.findProduct(id);
        if (current == null) {
            System.out.println("Product not found.");
            return;
        }
        int expectedVersion = current.getVersion();
        RegularProduct updated = readProduct(id, current);
        if (controller.updateProduct(updated, expectedVersion)) {
            System.out.println("Update successful. New version: " + updated.getVersion());
        } else {
            System.out.println("The data changed in another session. Reload and try again.");
        }
    }

    public void deleteProduct() {
        String id = input.readStringRequired("Product ID to delete");
        if (controller.findProduct(id) == null) {
            System.out.println("Product not found.");
            return;
        }
        String confirmation = input.readLine("Enter DELETE to confirm: ");
        if (!"DELETE".equalsIgnoreCase(confirmation)) {
            System.out.println("Deletion cancelled.");
            return;
        }
        System.out.println(controller.deleteProduct(id) ? "Product deleted." : "Deletion failed.");
    }

    public void searchProducts() {
        String category = input.readLine("Category (leave blank for all): ");
        int minimumPrice = input.readIntMin("Minimum price", 0, "Price cannot be negative");
        int maximumPrice = input.readIntMin("Maximum price", minimumPrice,
                "Maximum price must be greater than or equal to minimum price.");
        displayProducts(controller.searchProducts(category, minimumPrice, maximumPrice));
    }

    public RegularProduct readProduct(String id, RegularProduct current) {
        String name = readTextOrDefault("Name", current == null ? null : current.getName());
        String brand = readTextOrDefault("Brand", current == null ? null : current.getBrand());
        String category = readTextOrDefault("Category", current == null ? null : current.getCategory());
        int price = readIntOrDefault("Price", current == null ? null
                : current.getPrice().setScale(0, java.math.RoundingMode.HALF_UP).intValueExact(), 1);
        int stock = readIntOrDefault("Stock", current == null ? null : current.getStock(), 0);
        String description = readTextOrDefault("Description",
                current == null ? "" : current.getDescription());
        int version = current == null ? 0 : current.getVersion();
        RegularProduct product = new RegularProduct(id, name, description, category,
                BigDecimal.valueOf(price), stock, version,
                current == null ? "ACTIVE" : current.getStatus());
        product.setBrand(brand);
        return product;
    }

    public String readTextOrDefault(String label, String current) {
        if (current == null) {
            return input.readStringRequired(label);
        }
        String value = input.readLine(label + " [" + current + "]: ");
        return value.isBlank() ? current : value;
    }

    public int readIntOrDefault(String label, Integer current, int minimum) {
        if (current == null) {
            return input.readIntMin(label, minimum, label + " is invalid");
        }
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

    public void displayProducts(List<RegularProduct> products) {
        if (products.isEmpty()) {
            System.out.println("No matching products found.");
            return;
        }
        System.out.printf("%-10s %-25s %-15s %-15s %12s %8s %8s%n",
                "ID", "Name", "Brand", "Category", "Price", "Stock", "Version");
        System.out.println("------------------------------------------------------------------------------------------------------");
        products.stream().limit(DISPLAY_LIMIT).forEach(product ->
                System.out.printf("%-10s %-25s %-15s %-15s %,12.0f %8d %8d%n",
                        product.getProductId(), trim(product.getName(), 25),
                        trim(product.getBrand(), 15), trim(product.getCategory(), 15),
                        product.getPrice().setScale(0, java.math.RoundingMode.HALF_UP),
                        product.getStock(), product.getVersion()));
        if (products.size() > DISPLAY_LIMIT) {
            System.out.println("... and " + (products.size() - DISPLAY_LIMIT) + " products.");
        }
    }

    public static String trim(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value == null ? "" : value;
        }
        return value.substring(0, limit - 3) + "...";
    }
}

// Thành viên 3

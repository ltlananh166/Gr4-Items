package view;

import model.RegularProduct;
import repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProductAdminView {
    private static final int DISPLAY_LIMIT = 20;

    private final ProductRepository productRepository;
    private final ConsoleInput input;

    public ProductAdminView(ProductRepository productRepository, ConsoleInput input) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== PRODUCT DATA =====");
            System.out.println("1. List products");
            System.out.println("2. Search by ID, name, brand, or category");
            System.out.println("3. Add product");
            System.out.println("4. Edit product");
            System.out.println("5. Delete product");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Choose an option", 0, 5,
                    "Choose a number from 0 to 5.");
            switch (choice) {
                case 1 -> displayProducts(productRepository.findAll(), "Products");
                case 2 -> searchProducts();
                case 3 -> addProduct();
                case 4 -> editProduct();
                case 5 -> deleteProduct();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated product menu choice was out of range.");
            }
        }
    }

    private void addProduct() {
        RegularProduct product = new RegularProduct();
        product.setProductId(productRepository.generateNewProductId());
        product.setName(input.readStringRequired("Product name: "));
        product.setBrand(input.readStringRequired("Brand: "));
        product.setCategory(input.readStringRequired("Category: "));
        product.setPrice(readPrice("Price: "));
        product.setStock(input.readIntMin("Stock: ", 0, "Stock cannot be negative."));
        product.setDescription(input.readStringRequired("Description: "));
        product.setVersion(0);
        productRepository.save(product);
        System.out.println("Created product " + product.getProductId() + ".");
    }

    private void editProduct() {
        String productId = input.readStringRequired("Product ID to edit: ");
        RegularProduct product = productRepository.findById(productId);
        if (product == null) {
            System.out.println("Product not found: " + productId);
            return;
        }

        int expectedVersion = product.getVersion();
        String value = input.readLine("Name [" + product.getName() + "]: ");
        if (!value.isEmpty()) product.setName(value);
        value = input.readLine("Brand [" + product.getBrand() + "]: ");
        if (!value.isEmpty()) product.setBrand(value);
        value = input.readLine("Category [" + product.getCategory() + "]: ");
        if (!value.isEmpty()) product.setCategory(value);
        product.setPrice(readOptionalPrice(product.getPrice()));
        product.setStock(readOptionalStock(product.getStock()));
        value = input.readLine("Description [" + product.getDescription() + "]: ");
        if (!value.isEmpty()) product.setDescription(value);
        product.setVersion(expectedVersion + 1);

        if (productRepository.update(product, expectedVersion)) {
            System.out.println("Updated product " + productId + ".");
        } else {
            System.out.println("Product changed during editing; reload it and try again.");
        }
    }

    private void deleteProduct() {
        String productId = input.readStringRequired("Product ID to delete: ");
        String confirmation = input.readLine(
                "Delete " + productId + "? Type yes to confirm: ");
        if (!"yes".equalsIgnoreCase(confirmation)) {
            System.out.println("Delete cancelled.");
            return;
        }
        System.out.println(productRepository.deleteById(productId)
                ? "Deleted product " + productId + "."
                : "Product not found: " + productId);
    }

    private void searchProducts() {
        String query = input.readStringRequired("Search products: ")
                .toLowerCase(Locale.ROOT);
        List<RegularProduct> matches = productRepository.findAll().stream()
                .filter(product -> contains(product.getProductId(), query)
                        || contains(product.getName(), query)
                        || contains(product.getBrand(), query)
                        || contains(product.getCategory(), query))
                .toList();
        displayProducts(matches, "Search results");
    }

    private void displayProducts(List<RegularProduct> products, String title) {
        System.out.printf("%s: %d%n", title, products.size());
        int shown = Math.min(DISPLAY_LIMIT, products.size());
        System.out.printf("%-10s %-32s %-14s %-18s %12s %8s%n",
                "Product ID", "Name", "Brand", "Category", "Price", "Stock");
        for (int index = 0; index < shown; index++) {
            RegularProduct product = products.get(index);
            System.out.printf("%-10s %-32s %-14s %-18s %,12.0f %8d%n",
                    product.getProductId(), trim(product.getName(), 32),
                    trim(product.getBrand(), 14), trim(product.getCategory(), 18),
                    product.getPrice().doubleValue(), product.getStock());
        }
        if (products.size() > shown) {
            System.out.printf("Showing first %d results; refine your search to see a specific product.%n",
                    shown);
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private BigDecimal readPrice(String prompt) {
        while (true) {
            String value = input.readStringRequired(prompt);
            try {
                BigDecimal price = new BigDecimal(value);
                if (price.signum() >= 0) {
                    return price;
                }
            } catch (NumberFormatException ignored) {
                // Prompt again with a validation message.
            }
            System.out.println("Enter a valid non-negative price.");
        }
    }

    private BigDecimal readOptionalPrice(BigDecimal currentPrice) {
        while (true) {
            String value = input.readLine("Price [" + currentPrice + "]: ");
            if (value.isEmpty()) return currentPrice;
            try {
                BigDecimal price = new BigDecimal(value);
                if (price.signum() >= 0) return price;
            } catch (NumberFormatException ignored) {
                // Prompt again with a validation message.
            }
            System.out.println("Enter a valid non-negative price.");
        }
    }

    private int readOptionalStock(int currentStock) {
        while (true) {
            String value = input.readLine("Stock [" + currentStock + "]: ");
            if (value.isEmpty()) return currentStock;
            try {
                int stock = Integer.parseInt(value);
                if (stock >= 0) return stock;
            } catch (NumberFormatException ignored) {
                // Prompt again with a validation message.
            }
            System.out.println("Enter a valid non-negative stock quantity.");
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}

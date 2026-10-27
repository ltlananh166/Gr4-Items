package view;

import controller.SimulatorController;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import model.Cart;
import model.CartItem;
import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderTransaction;
import model.RegularProduct;
import model.SimulationResult;
import model.Voucher;
import model.enums.CustomerStatus;
import model.enums.LockMechanism;
import model.enums.VoucherStatus;
import service.FlashSalePlatform;

public class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final FlashSalePlatform platform =
            new FlashSalePlatform("data/customers.csv", "data/vouchers.csv");
    private final SimulatorController simulator = new SimulatorController();
    private Customer currentCustomer;
    private int simulationThreads = 50;
    private LockMechanism simulationMechanism = LockMechanism.SYNCHRONIZED;
    private SimulationResult lastSimulationResult;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== FLASH SALE SYSTEM ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. View regular products");
            System.out.println("4. Search products");
            System.out.println("5. View product details");
            System.out.println("6. View Flash Sale events and products");
            System.out.println("7. Administrator menu");
            System.out.println("8. Researcher / simulator menu");
            System.out.println("0. Exit");
            int choice = readInt("Choose: ");
            try {
                switch (choice) {
                    case 1:
                        register();
                        break;
                    case 2:
                        login();
                        break;
                    case 3:
                        printProducts(platform.listRegularProducts());
                        break;
                    case 4:
                        searchCatalog();
                        break;
                    case 5:
                        showProductDetails(readLine("Product ID: "));
                        break;
                    case 6:
                        showFlashSales();
                        break;
                    case 7:
                        adminMenu();
                        break;
                    case 8:
                        researcherMenu();
                        break;
                    case 0:
                        running = false;
                        System.out.println("Goodbye.");
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Action failed: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private void register() {
        System.out.println("\n--- REGISTER ---");
        Customer customer = platform.register(readLine("Name: "), readLine("Email: "),
                readLine("Phone: "), readLine("Address: "), readLine("Password: "));
        System.out.println("Account created: " + customer.getCustomerId());
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");
        currentCustomer = platform.login(readLine("Email: "), readLine("Password: "));
        System.out.println("Welcome, " + currentCustomer.getName()
                + " (" + currentCustomer.getTier() + ").");
        customerMenu();
    }

    private void customerMenu() {
        boolean loggedIn = true;
        while (loggedIn && currentCustomer != null) {
            System.out.println("\n=== CUSTOMER MENU ===");
            System.out.println("1. View regular products");
            System.out.println("2. Search products");
            System.out.println("3. View product details");
            System.out.println("4. View Flash Sale events/products");
            System.out.println("5. Add product to cart");
            System.out.println("6. View cart");
            System.out.println("7. Apply voucher (preview)");
            System.out.println("8. Place order");
            System.out.println("9. Make payment");
            System.out.println("10. View order history");
            System.out.println("11. Cancel unpaid order");
            System.out.println("12. View transaction history");
            System.out.println("13. Top up wallet");
            System.out.println("0. Log out");
            int choice = readInt("Choose: ");
            try {
                switch (choice) {
                    case 1:
                        printProducts(platform.listRegularProducts());
                        break;
                    case 2:
                        searchCatalog();
                        break;
                    case 3:
                        showProductDetails(readLine("Product ID: "));
                        break;
                    case 4:
                        showFlashSales();
                        break;
                    case 5:
                        addToCart();
                        break;
                    case 6:
                        showCart();
                        break;
                    case 7:
                        applyVoucher();
                        break;
                    case 8:
                        placeOrder();
                        break;
                    case 9:
                        makePayment();
                        break;
                    case 10:
                        showOrderHistory();
                        break;
                    case 11:
                        platform.cancelCustomerOrder(currentCustomer.getCustomerId(),
                                readLine("Order ID: "));
                        System.out.println("Order cancelled.");
                        break;
                    case 12:
                        showTransactionHistory();
                        break;
                    case 13:
                        double balance = platform.topUpWallet(currentCustomer.getCustomerId(),
                                readDecimal("Top-up amount: ").doubleValue());
                        currentCustomer.setWalletBalance(balance);
                        System.out.println("Wallet balance: " + balance);
                        break;
                    case 0:
                        currentCustomer = null;
                        loggedIn = false;
                        System.out.println("Logged out.");
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Action failed: " + e.getMessage());
            }
        }
    }

    private void addToCart() {
        boolean flashSale = readInt("1. Regular product  2. Flash Sale item: ") == 2;
        String id = readLine(flashSale ? "Flash Sale item ID: " : "Product ID: ");
        int quantity = readInt("Quantity: ");
        Cart cart = platform.addToCart(currentCustomer.getCustomerId(), id, quantity, flashSale);
        System.out.println("Cart updated. Total: " + cart.getTotalAmount());
    }

    private void showCart() {
        Cart cart = platform.viewCart(currentCustomer.getCustomerId());
        if (cart.getItems().isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        for (CartItem item : cart.getItems()) {
            System.out.printf("%s x%d @ %s = %s%n", item.getProductId(), item.getQuantity(),
                    item.getUnitPrice(), item.calculateSubtotal());
        }
        System.out.println("Total: " + cart.getTotalAmount());
    }

    private void applyVoucher() {
        String code = readLine("Voucher code: ");
        BigDecimal finalAmount = platform.previewVoucher(currentCustomer.getCustomerId(), code);
        System.out.println("Voucher is valid. Estimated total after tier and voucher discounts: "
                + finalAmount);
    }

    private void placeOrder() {
        showCart();
        String voucher = readLine("Voucher code (leave blank for none): ");
        Order order = platform.placeOrder(currentCustomer.getCustomerId(), voucher);
        System.out.println("Order placed: " + order.getOrderId() + ", amount due: "
                + order.getFinalAmount() + ", status: " + order.getStatus());
    }

    private void makePayment() {
        String orderId = readLine("Order ID: ");
        String method = readLine("Payment method (WALLET/VNPAY): ");
        OrderTransaction transaction = platform.makePayment(
                currentCustomer.getCustomerId(), orderId, method);
        System.out.println("Payment " + transaction.getStatus() + ". Transaction: "
                + transaction.getId());
    }

    private void showOrderHistory() {
        List<Order> orders = platform.viewOrderHistory(currentCustomer.getCustomerId());
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }
        for (Order order : orders) {
            System.out.printf("%s | status=%s | total=%s | discount=%s | due=%s%n",
                    order.getOrderId(), order.getStatus(), order.getTotalAmount(),
                    order.getDiscountAmount(), order.getFinalAmount());
        }
    }

    private void showTransactionHistory() {
        List<OrderTransaction> transactions =
                platform.viewTransactionHistory(currentCustomer.getCustomerId());
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        transactions.forEach(transaction -> System.out.printf(
                "%s | order=%s | %s | %s | %s%n", transaction.getId(),
                transaction.getOrderId(), transaction.getAmount(), transaction.getPaymentMethod(),
                transaction.getStatus()));
    }

    private void showProductDetails(String productId) {
        if (platform.findFlashSaleItem(productId).isPresent()) {
            FlashSaleItem item = platform.findFlashSaleItem(productId).get();
            FlashSaleEvent event = platform.listFlashSaleEvents().stream()
                    .filter(candidate -> candidate.getEventId().equals(item.getEventId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Sale event does not exist."));
            System.out.printf("%s | %s | regular price=%s | sale price=%s | stock=%d | event=%s | active=%s%n",
                    item.getId(), item.getProductName(), item.getOriginalPrice(),
                    item.getSalePrice(), item.getStock(), event.getTitle(), event.isActive());
            return;
        }
        RegularProduct product = platform.findProduct(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist."));
        System.out.printf("%s | %s | %s | %s | price=%s | stock=%d | status=%s%n",
                product.getProductId(), product.getName(), product.getCategory(),
                product.getDescription(), product.getPrice(), product.getStock(), product.getStatus());
    }

    private void searchCatalog() {
        String term = readLine("Search: ");
        printProducts(platform.searchProducts(term));
        List<FlashSaleItem> matches = platform.searchFlashSaleItems(term);
        if (!matches.isEmpty()) {
            System.out.println("Matching Flash Sale products:");
            matches.forEach(this::printFlashSaleItem);
        }
    }

    private void showFlashSales() {
        for (FlashSaleEvent event : platform.listFlashSaleEvents()) {
            System.out.printf("%s | %s | %s | %s - %s | active=%s%n",
                    event.getEventId(), event.getTitle(), event.getStatus(),
                    event.getStartTime(), event.getEndTime(), event.isActive());
            for (FlashSaleItem item : platform.listFlashSaleItems(event.getEventId())) {
                System.out.printf("  %s | product=%s | %s | price=%s | stock=%d | limit=%d%n",
                        item.getId(), item.getProductName(), item.getProductId(),
                        item.getSalePrice(), item.getStock(), item.getMaxPerUser());
            }
        }
    }

    private void adminMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== ADMINISTRATOR MENU ===");
            System.out.println("1. View/search products");
            System.out.println("2. Add product");
            System.out.println("3. Update product");
            System.out.println("4. Delete product");
            System.out.println("5. View users");
            System.out.println("6. Create user");
            System.out.println("7. Update user");
            System.out.println("8. Delete user");
            System.out.println("9. Ban/unban user");
            System.out.println("10. Add/reduce inventory");
            System.out.println("11. View inventory availability");
            System.out.println("12. Create Flash Sale");
            System.out.println("13. Update Flash Sale");
            System.out.println("14. End Flash Sale");
            System.out.println("15. Cancel Flash Sale");
            System.out.println("16. Search Flash Sales");
            System.out.println("17. Add product to Flash Sale");
            System.out.println("18. List vouchers");
            System.out.println("19. Create voucher");
            System.out.println("20. Update voucher");
            System.out.println("21. Delete voucher");
            System.out.println("22. View orders");
            System.out.println("23. Approve order");
            System.out.println("24. Cancel order");
            System.out.println("25. View sales report");
            System.out.println("0. Back");
            int choice = readInt("Choose: ");
            try {
                switch (choice) {
                    case 1:
                        String search = readLine("Search term (blank for all): ");
                        printProducts(search.isEmpty() ? platform.listRegularProducts()
                                : platform.searchProducts(search));
                        break;
                    case 2:
                        createProduct();
                        break;
                    case 3:
                        updateProduct();
                        break;
                    case 4:
                        System.out.println(platform.deleteProduct(readLine("Product ID: "))
                                ? "Product deleted." : "Product not found.");
                        break;
                    case 5:
                        platform.viewUsers().forEach(this::printCustomer);
                        break;
                    case 6:
                        createUser();
                        break;
                    case 7:
                        updateUser();
                        break;
                    case 8:
                        System.out.println(platform.deleteUser(readLine("Customer ID: "))
                                ? "User deleted." : "User not found.");
                        break;
                    case 9:
                        updateUserStatus();
                        break;
                    case 10:
                        changeInventory();
                        break;
                    case 11:
                        showInventory();
                        break;
                    case 12:
                        createFlashSale();
                        break;
                    case 13:
                        updateFlashSale();
                        break;
                    case 14:
                        platform.endFlashSale(readLine("Event ID: "));
                        System.out.println("Flash Sale ended.");
                        break;
                    case 15:
                        platform.cancelFlashSale(readLine("Event ID: "));
                        System.out.println("Flash Sale cancelled.");
                        break;
                    case 16:
                        platform.searchFlashSales(readLine("Search event: "))
                                .forEach(event -> {
                                    System.out.printf("%s | %s | %s%n",
                                            event.getEventId(), event.getTitle(), event.getStatus());
                                    platform.listFlashSaleItems(event.getEventId())
                                            .forEach(this::printFlashSaleItem);
                                });
                        break;
                    case 17:
                        addFlashSaleItem();
                        break;
                    case 18:
                        platform.listVouchers().forEach(this::printVoucher);
                        break;
                    case 19:
                        createVoucher();
                        break;
                    case 20:
                        updateVoucher();
                        break;
                    case 21:
                        System.out.println(platform.deleteVoucher(readLine("Voucher ID: "))
                                ? "Voucher deleted." : "Voucher not found.");
                        break;
                    case 22:
                        platform.listOrders().forEach(this::printOrder);
                        break;
                    case 23:
                        printOrder(platform.approveOrder(readLine("Order ID: ")));
                        break;
                    case 24:
                        platform.cancelOrderAsAdmin(readLine("Order ID: "));
                        System.out.println("Order cancelled.");
                        break;
                    case 25:
                        printSalesReport();
                        break;
                    case 0:
                        active = false;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Action failed: " + e.getMessage());
            }
        }
    }

    private void createProduct() {
        RegularProduct product = platform.createProduct(readLine("Name: "), readLine("Description: "),
                readLine("Category: "), readDecimal("Price: "), readInt("Initial stock: "));
        System.out.println("Created product " + product.getProductId());
    }

    private void updateProduct() {
        String id = readLine("Product ID: ");
        RegularProduct product = platform.updateProduct(id, readLine("Name: "),
                readLine("Description: "), readLine("Category: "),
                readDecimal("Price: "), readLine("Status (ACTIVE/INACTIVE): "));
        System.out.println("Updated " + product.getProductId());
    }

    private void createUser() {
        Customer customer = platform.createUser(readLine("Name: "), readLine("Email: "),
                readLine("Phone: "), readLine("Address: "), readLine("Password: "));
        System.out.println("Created customer " + customer.getCustomerId());
    }

    private void updateUser() {
        Customer customer = platform.updateUser(readLine("Customer ID: "), readLine("Name: "),
                readLine("Phone: "), readLine("Address: "));
        printCustomer(customer);
    }

    private void updateUserStatus() {
        String id = readLine("Customer ID: ");
        CustomerStatus status = readInt("1. Ban  2. Unban: ") == 1
                ? CustomerStatus.BANNED : CustomerStatus.ACTIVE;
        printCustomer(platform.setUserStatus(id, status));
    }

    private void changeInventory() {
        String id = readLine("Product ID (or FLASH:<item ID>): ");
        int adjustment = readInt("Stock change (+ to add, - to reduce): ");
        System.out.println("Available stock: " + platform.changeInventory(id, adjustment));
    }

    private void showInventory() {
        printProducts(platform.listRegularProducts());
        platform.listFlashSaleItems(null).forEach(this::printFlashSaleItem);
    }

    private void createFlashSale() {
        FlashSaleEvent event = platform.createFlashSale(readLine("Title: "),
                readLine("Description: "), readDateTime("Start (yyyy-MM-ddTHH:mm): "),
                readDateTime("End (yyyy-MM-ddTHH:mm): "));
        System.out.println("Created event " + event.getEventId());
    }

    private void updateFlashSale() {
        FlashSaleEvent event = platform.updateFlashSale(readLine("Event ID: "),
                readLine("Title: "), readLine("Description: "),
                readDateTime("Start (yyyy-MM-ddTHH:mm): "),
                readDateTime("End (yyyy-MM-ddTHH:mm): "));
        System.out.println("Updated event " + event.getEventId());
    }

    private void addFlashSaleItem() {
        FlashSaleItem item = platform.addFlashSaleProduct(readLine("Event ID: "),
                readLine("Product ID: "), readDecimal("Sale price: "),
                readInt("Sale quantity: "), readInt("Max per customer: "));
        System.out.println("Added Flash Sale item " + item.getId());
    }

    private void createVoucher() {
        Voucher voucher = platform.createVoucher(readLine("Code: "),
                readLine("Type (FIXED/PERCENTAGE): "), readDecimal("Discount amount: "),
                readDecimal("Minimum order: "), readDecimal("Maximum discount (0 = unlimited): "),
                readOptionalDateTime("Valid from (blank = now): "),
                readOptionalDateTime("Valid to (blank = no expiry): "));
        System.out.println("Created voucher " + voucher.getVoucherId());
    }

    private void updateVoucher() {
        String id = readLine("Voucher ID: ");
        VoucherStatus status = VoucherStatus.valueOf(
                readLine("Status (ACTIVE/INACTIVE/USED): ").trim().toUpperCase());
        Voucher voucher = platform.updateVoucher(id, readLine("Code: "),
                readLine("Type (FIXED/PERCENTAGE): "), readDecimal("Discount amount: "),
                readDecimal("Minimum order: "), readDecimal("Maximum discount: "),
                readOptionalDateTime("Valid from (blank = none): "),
                readOptionalDateTime("Valid to (blank = none): "), status);
        System.out.println("Updated voucher " + voucher.getVoucherId());
    }

    private void printSalesReport() {
        for (Map.Entry<String, BigDecimal> entry : platform.salesReport().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private void researcherMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n=== RESEARCHER / SIMULATOR ===");
            System.out.println("1. Configure thread count");
            System.out.println("2. Select lock mechanism");
            System.out.println("3. Run Flash Sale simulation");
            System.out.println("4. View latest simulation result");
            System.out.println("5. Export transaction report");
            System.out.println("0. Back");
            int choice = readInt("Choose: ");
            try {
                switch (choice) {
                    case 1:
                        simulationThreads = readInt("Concurrent threads (1-2000): ");
                        if (simulationThreads < 1 || simulationThreads > 2000) {
                            throw new IllegalArgumentException("Thread count must be between 1 and 2000.");
                        }
                        System.out.println("Configured " + simulationThreads + " threads.");
                        break;
                    case 2:
                        simulationMechanism = selectLockMechanism();
                        System.out.println("Selected " + simulationMechanism);
                        break;
                    case 3:
                        simulator.resetStock("ITEM001", readInt("Reset stock: "));
                        lastSimulationResult = simulator.runSimulation("ITEM001", simulationThreads,
                                1, simulationMechanism);
                        if (lastSimulationResult == null) {
                            System.out.println("Simulation was interrupted.");
                        } else {
                            printSimulationResult(lastSimulationResult);
                        }
                        break;
                    case 4:
                        if (lastSimulationResult == null) {
                            System.out.println("No simulation result is available.");
                        } else {
                            printSimulationResult(lastSimulationResult);
                        }
                        break;
                    case 5:
                        String path = readLine("Output CSV path: ");
                        simulator.exportResults(Paths.get(path));
                        System.out.println("Simulation records exported to " + path);
                        break;
                    case 0:
                        active = false;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (RuntimeException | java.io.IOException e) {
                System.out.println("Action failed: " + e.getMessage());
            }
        }
    }

    private LockMechanism selectLockMechanism() {
        LockMechanism[] mechanisms = LockMechanism.values();
        for (int i = 0; i < mechanisms.length; i++) {
            System.out.println((i + 1) + ". " + mechanisms[i]);
        }
        int choice = readInt("Choose: ");
        if (choice < 1 || choice > mechanisms.length) {
            throw new IllegalArgumentException("Invalid lock mechanism.");
        }
        return mechanisms[choice - 1];
    }

    private void printSimulationResult(SimulationResult result) {
        System.out.printf("Mechanism=%s, threads=%d, requests=%d, succeeded=%d, failed=%d, "
                        + "oversold=%d, elapsed=%.2fms, TPS=%.2f%n",
                result.getLockMechanism(), result.getThreads(), result.getTotalRequests(),
                result.getSuccessCount(), result.getFailedCount(), result.getNegativeStockCount(),
                result.getElapsedMillis(), result.getTps());
    }

    private void printProducts(List<RegularProduct> products) {
        if (products.isEmpty()) {
            System.out.println("No products found.");
            return;
        }
        products.forEach(product -> System.out.printf(
                "%s | %s | %s | price=%s | stock=%d | %s%n",
                product.getProductId(), product.getName(), product.getCategory(),
                product.getPrice(), product.getStock(), product.getStatus()));
    }

    private void printCustomer(Customer customer) {
        System.out.printf("%s | %s | %s | %s | %s | tier=%s%n",
                customer.getCustomerId(), customer.getName(), customer.getEmail(),
                customer.getPhone(), customer.getStatus(), customer.getTier());
    }

    private void printFlashSaleItem(FlashSaleItem item) {
        System.out.printf("%s | event=%s | product=%s | sale=%s | stock=%d%n",
                item.getId(), item.getEventId(), item.getProductName(),
                item.getSalePrice(), item.getStock());
    }

    private void printVoucher(Voucher voucher) {
        System.out.printf("%s | %s | %s %s | min=%s | max=%s | %s%n",
                voucher.getVoucherId(), voucher.getCode(), voucher.getDiscountAmount(),
                voucher.getDiscountType(), voucher.getMinOrderValue(),
                voucher.getMaxDiscountValue(), voucher.getStatus());
    }

    private void printOrder(Order order) {
        System.out.printf("%s | customer=%s | status=%s | total=%s | due=%s%n",
                order.getOrderId(), order.getCustomerId(), order.getStatus(),
                order.getTotalAmount(), order.getFinalAmount());
    }

    private int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    private BigDecimal readDecimal(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid decimal number.");
            }
        }
    }

    private LocalDateTime readDateTime(String prompt) {
        while (true) {
            try {
                return LocalDateTime.parse(readLine(prompt));
            } catch (DateTimeParseException e) {
                System.out.println("Use ISO local date-time format, for example 2026-10-28T09:30.");
            }
        }
    }

    private LocalDateTime readOptionalDateTime(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("Use ISO local date-time format or leave blank.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}

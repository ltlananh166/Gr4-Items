package view;

import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.RegularProduct;
import model.Voucher;
import model.enums.CustTier;
import model.enums.OrderStatus;
import model.enums.SaleStatus;
import model.enums.TransactionStatus;
import model.enums.VoucherStatus;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.VoucherRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Admin-facing console for the CSV-backed data repositories. */
public class DataManagementView {
    private static final DateTimeFormatter INPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConsoleInput input;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final VoucherRepository vouchers;
    private final FlashSaleItemRepository flashItems;
    private final FlashSaleEventRepository events;
    private final OrderDetailRepository orderDetails;
    private final OrderTransactionRepository transactions;

    public DataManagementView(
            ConsoleInput input,
            CustomerRepository customers,
            ProductRepository products,
            OrderRepository orders,
            VoucherRepository vouchers,
            FlashSaleItemRepository flashItems,
            FlashSaleEventRepository events,
            OrderDetailRepository orderDetails,
            OrderTransactionRepository transactions) {
        this.input = input;
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.vouchers = vouchers;
        this.flashItems = flashItems;
        this.events = events;
        this.orderDetails = orderDetails;
        this.transactions = transactions;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== DATA MANAGEMENT =====");
            System.out.println("1. Customers");
            System.out.println("2. Products");
            System.out.println("3. Orders");
            System.out.println("4. Vouchers");
            System.out.println("5. Flash Sale Items");
            System.out.println("6. Flash Sale Events");
            System.out.println("7. Order Details");
            System.out.println("8. Transactions");
            System.out.println("9. Repository overview");
            System.out.println("0. Back to Admin menu");

            switch (input.readInt("Choose an option", 0)) {
                case 1 -> customersMenu();
                case 2 -> productsMenu();
                case 3 -> ordersMenu();
                case 4 -> vouchersMenu();
                case 5 -> flashItemsMenu();
                case 6 -> eventsMenu();
                case 7 -> orderDetailsMenu();
                case 8 -> transactionsMenu();
                case 9 -> showRepositoryOverview();
                case 0 -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void customersMenu() {
        runTable("CUSTOMERS", "data/customers.csv", customers::getAll,
                id -> customers.findById(id).orElse(null),
                customer -> String.format(
                        "%s | %s | %s | %s | %s | wallet=%.0f",
                        customer.getCustomerId(), customer.getName(),
                        customer.getEmail(), customer.getPhone(),
                        customer.getStatus(), customer.getWalletBalance()),
                this::addCustomer);
    }

    private void productsMenu() {
        runTable("PRODUCTS", products.getFilePath().toString(), products::findAll,
                products::findById, product -> String.format(
                        "%s | %s | category=%s | price=%s | stock=%d | %s",
                        product.getProductId(), product.getName(),
                        product.getCategory(), product.getPrice(),
                        product.getStock(), product.getStatus()),
                this::addProduct);
    }

    private void ordersMenu() {
        runTable("ORDERS", orders.getFilePath().toString(), orders::findAll,
                orders::findById, order -> String.format(
                        "%s | customer=%s | status=%s | total=%s | discount=%s",
                        order.getOrderId(), order.getCustomerId(),
                        order.getStatus(), order.getTotalAmount(),
                        order.getDiscountAmount()),
                this::addOrder);
    }

    private void vouchersMenu() {
        runTable("VOUCHERS", "data/vouchers.csv", vouchers::getAll,
                id -> vouchers.findById(id).orElse(null), voucher -> String.format(
                        "%s | %s | %s %s | min=%s | status=%s",
                        voucher.getVoucherId(), voucher.getCode(),
                        voucher.getDiscountType(), voucher.getDiscountAmount(),
                        voucher.getMinOrderValue(), voucher.getStatus()),
                this::addVoucher);
    }

    private void flashItemsMenu() {
        runTable("FLASH SALE ITEMS", flashItems.getFilePath().toString(),
                flashItems::findAll, flashItems::findById,
                item -> String.format("%s | %s | event=%s | stock=%d | price=%.2f",
                        item.getId(), item.getProductName(), item.getEventId(),
                        item.getStock(), item.getPrice()),
                this::addFlashItem);
    }

    private void eventsMenu() {
        runTable("FLASH SALE EVENTS", events.getFilePath().toString(), events::findAll,
                events::findById, event -> String.format(
                        "%s | %s | %s to %s | %s",
                        event.getEventId(), event.getTitle(), event.getStartTime(),
                        event.getEndTime(), event.getStatus()),
                this::addEvent);
    }

    private void orderDetailsMenu() {
        runTable("ORDER DETAILS", orderDetails.getFilePath().toString(),
                orderDetails::findAll, orderDetails::findById,
                detail -> String.format("%s | order=%s | product=%s | qty=%d | unit=%s",
                        detail.getOrderDetailId(), detail.getOrderId(),
                        detail.getProductId(), detail.getQuantity(), detail.getUnitPrice()),
                this::addOrderDetail);
    }

    private void transactionsMenu() {
        runTable("TRANSACTIONS", transactions.getFilePath().toString(),
                transactions::findAll, transactions::findById,
                transaction -> String.format("%s | order=%s | customer=%s | amount=%s | %s | %s",
                        transaction.getId(), transaction.getOrderId(),
                        transaction.getCustomerId(), transaction.getAmount(),
                        transaction.getPaymentMethod(), transaction.getStatus()),
                this::addTransaction);
    }

    private <T> void runTable(
            String title,
            String file,
            Supplier<List<T>> listAll,
            Function<String, T> findById,
            Function<T, String> formatter,
            Runnable add) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- " + title + " ---");
            System.out.println("Data: " + file);
            System.out.println("1. List all");
            System.out.println("2. Find by ID");
            System.out.println("3. Add");
            System.out.println("0. Back");
            switch (input.readInt("Choose an option", 0)) {
                case 1 -> printRows(listAll.get(), formatter);
                case 2 -> {
                    String id = input.readStringRequired("ID: ");
                    T row = findById.apply(id);
                    System.out.println(row == null
                            ? "No record found for ID " + id
                            : formatter.apply(row));
                }
                case 3 -> {
                    try {
                        add.run();
                    } catch (RuntimeException e) {
                        System.out.println("Could not add record: " + e.getMessage());
                    }
                }
                case 0 -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private <T> void printRows(List<T> rows, Function<T, String> formatter) {
        if (rows.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        rows.forEach(row -> System.out.println(formatter.apply(row)));
        System.out.println("Total: " + rows.size());
    }

    private void addCustomer() {
        String id = customers.generateNewCustomerId();
        String name = input.readStringRequired("Name: ");
        String email = input.readStringRequired("Email: ");
        if (customers.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A customer with that email already exists.");
        }
        String phone = input.readStringRequired("Phone: ");
        String address = input.readStringRequired("Address: ");
        String avatar = input.readLine("Avatar URL (optional): ");
        String password = input.readStringRequired("Initial password: ");
        double wallet = readNonNegativeDouble("Wallet balance: ");
        Customer customer = new Customer(id, name, email, phone, address, avatar,
                CustTier.STANDARD, "ACTIVE", password, wallet);
        ensureNewId(id, value -> customers.findById(value).orElse(null));
        customers.save(customer);
        System.out.println("Customer added with ID " + id + ". Password is stored using the existing password hashing flow.");
    }

    private void addProduct() {
        String id = products.generateNewProductId();
        RegularProduct product = new RegularProduct(id,
                input.readStringRequired("Name: "),
                input.readStringRequired("Description: "),
                input.readStringRequired("Category: "),
                readNonNegativeDecimal("Price: "),
                input.readIntMin("Stock: ", 0, "Stock cannot be negative."),
                0, "ACTIVE");
        ensureNewId(id, products::findById);
        products.save(product);
        System.out.println("Product added with ID " + id + ".");
    }

    private void addOrder() {
        String id = orders.generateNewOrderId();
        Order order = new Order(id,
                input.readStringRequired("Customer ID: "), OrderStatus.PENDING);
        ensureNewId(id, orders::findById);
        orders.save(order);
        System.out.println("Order added with ID " + id + " (PENDING).");
    }

    private void addVoucher() {
        String id = input.readStringRequired("Voucher ID: ");
        String code = input.readStringRequired("Voucher code: ");
        if (vouchers.findById(id).isPresent()) {
            throw new IllegalArgumentException("Voucher ID already exists: " + id);
        }
        if (vouchers.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Voucher code already exists: " + code);
        }
        String type = input.readStringRequired("Discount type (FIXED/PERCENTAGE): ");
        BigDecimal amount = readNonNegativeDecimal("Discount amount: ");
        BigDecimal minimum = readNonNegativeDecimal("Minimum order value: ");
        BigDecimal maximum = readNonNegativeDecimal("Maximum discount value (0 = no cap): ");
        String startText = input.readLine("Valid from (dd/MM/yyyy HH:mm, blank = now): ");
        String endText = input.readLine("Valid to (dd/MM/yyyy HH:mm, blank = no expiry): ");
        LocalDateTime start = startText.isBlank() ? LocalDateTime.now()
                : LocalDateTime.parse(startText, INPUT_DATE_FORMAT);
        LocalDateTime end = endText.isBlank() ? null
                : LocalDateTime.parse(endText, INPUT_DATE_FORMAT);
        Voucher voucher = new Voucher(id, code, type, amount, minimum, maximum,
                start, end, VoucherStatus.ACTIVE);
        vouchers.save(voucher);
        System.out.println("Voucher added with ID " + id + ".");
    }

    private void addFlashItem() {
        String id = input.readStringRequired("Item ID: ");
        String eventId = input.readStringRequired("Event ID: ");
        String productName = input.readStringRequired("Product name: ");
        int stock = input.readIntMin("Stock: ", 0, "Stock cannot be negative.");
        double price = readNonNegativeDouble("Price: ");
        FlashSaleItem item = new FlashSaleItem(id, eventId, productName, stock, price, 1);
        if (!flashItems.add(item)) {
            throw new IllegalArgumentException("Flash sale item ID already exists: " + id);
        }
        System.out.println("Flash sale item added and saved.");
    }

    private void addEvent() {
        String id = events.generateNewEventId();
        String title = input.readStringRequired("Event title: ");
        String description = input.readLine("Description (optional): ");
        LocalDateTime start = readDateTime("Start time");
        LocalDateTime end = readDateTime("End time");
        FlashSaleEvent event = new FlashSaleEvent(
                id, title, description, start, end, SaleStatus.UPCOMING);
        ensureNewId(id, events::findById);
        events.save(event);
        System.out.println("Event added with ID " + id + ".");
    }

    private void addOrderDetail() {
        String id = orderDetails.generateNewOrderDetailId();
        OrderDetail detail = new OrderDetail(id,
                input.readStringRequired("Order ID: "),
                input.readStringRequired("Product ID: "),
                input.readIntMin("Quantity: ", 1, "Quantity must be at least 1."),
                readNonNegativeDecimal("Unit price: "));
        ensureNewId(id, orderDetails::findById);
        orderDetails.save(detail);
        System.out.println("Order detail added with ID " + id + ".");
    }

    private void addTransaction() {
        String id = transactions.generateNewTransactionId();
        OrderTransaction transaction = new OrderTransaction(id,
                input.readStringRequired("Order ID: "),
                input.readStringRequired("Customer ID: "),
                readNonNegativeDecimal("Amount: "),
                input.readStringRequired("Payment method: "),
                TransactionStatus.PENDING, LocalDateTime.now());
        ensureNewId(id, transactions::findById);
        transactions.save(transaction);
        System.out.println("Transaction added with ID " + id + " (PENDING).");
    }

    private void showRepositoryOverview() {
        System.out.println("\n--- REPOSITORY OVERVIEW ---");
        showFile("Customers", Paths.get("data/customers.csv"), customers.getAll().size());
        showFile("Products", products.getFilePath(), products.findAll().size());
        showFile("Orders", orders.getFilePath(), orders.findAll().size());
        showFile("Vouchers", Paths.get("data/vouchers.csv"), vouchers.getAll().size());
        showFile("Flash Sale Items", flashItems.getFilePath(), flashItems.findAll().size());
        showFile("Flash Sale Events", events.getFilePath(), events.findAll().size());
        showFile("Order Details", orderDetails.getFilePath(), orderDetails.findAll().size());
        showFile("Transactions", transactions.getFilePath(), transactions.findAll().size());
    }

    private void showFile(String name, Path path, int count) {
        String location = path == null ? "in-memory mock"
                : path.toAbsolutePath().normalize().toString()
                + (Files.exists(path) ? "" : " (created on first write)");
        System.out.printf("%-20s %5d records | %s%n", name, count, location);
    }

    private LocalDateTime readDateTime(String label) {
        String value = input.readDateTime(label);
        return LocalDateTime.parse(value, INPUT_DATE_FORMAT);
    }

    private BigDecimal readNonNegativeDecimal(String label) {
        while (true) {
            String value = input.readStringRequired(label);
            try {
                BigDecimal result = new BigDecimal(value);
                if (result.signum() >= 0) {
                    return result;
                }
            } catch (NumberFormatException ignored) {
                // Ask again below.
            }
            System.out.println("Enter a valid non-negative number.");
        }
    }

    private <T> void ensureNewId(String id, Function<String, T> lookup) {
        if (lookup.apply(id) != null) {
            throw new IllegalArgumentException("ID already exists: " + id);
        }
    }

    private double readNonNegativeDouble(String label) {
        while (true) {
            String value = input.readStringRequired(label);
            try {
                double result = Double.parseDouble(value);
                if (Double.isFinite(result) && result >= 0) {
                    return result;
                }
            } catch (NumberFormatException ignored) {
                // Ask again below.
            }
            System.out.println("Enter a valid non-negative number.");
        }
    }
}

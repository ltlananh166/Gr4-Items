package service;

import controller.AdminCustomerController;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import model.Cart;
import model.CartItem;
import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.RegularProduct;
import model.Voucher;
import model.enums.CustomerStatus;
import model.enums.OrderStatus;
import model.enums.SaleStatus;
import model.enums.TransactionStatus;
import model.enums.VoucherStatus;
import repository.CustomerRepository;
import repository.VoucherRepository;

public class FlashSalePlatform {
    private static final String FLASH_ITEM_PREFIX = "FLASH:";

    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerService customerService;
    private final AdminCustomerController adminCustomerController;
    private final Map<String, RegularProduct> products = new LinkedHashMap<>();
    private final Map<String, FlashSaleEvent> events = new LinkedHashMap<>();
    private final Map<String, FlashSaleItem> flashSaleItems = new LinkedHashMap<>();
    private final Map<String, Cart> carts = new LinkedHashMap<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<String, OrderTransaction> transactions = new LinkedHashMap<>();
    private final Map<String, String> orderVouchers = new LinkedHashMap<>();
    private final Map<String, String> orderPaymentMethods = new LinkedHashMap<>();
    private int productSequence = 4;
    private int eventSequence = 2;
    private int flashItemSequence = 2;
    private int orderSequence = 1;
    private int transactionSequence = 1;
    private int cartItemSequence = 1;

    public FlashSalePlatform(String customerFile, String voucherFile) {
        customerRepository = new CustomerRepository(customerFile);
        voucherRepository = new VoucherRepository(voucherFile);
        customerService = new CustomerService(customerRepository, voucherRepository);
        adminCustomerController = new AdminCustomerController(customerRepository);
        seedCatalog();
    }

    private static int quantityInCart(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(item -> productId.equals(item.getProductId()))
                .mapToInt(CartItem::getQuantity).sum();
    }

    public Customer register(String name, String email, String phone, String address, String password) {
        return customerService.register(name, email, phone, address, password);
    }

    public Customer login(String email, String password) {
        return customerService.login(email, password);
    }

    public double topUpWallet(String customerId, double amount) {
        return customerService.topUpWallet(customerId, amount);
    }

    public List<RegularProduct> listRegularProducts() {
        return products.values().stream().collect(Collectors.toList());
    }

    public List<RegularProduct> searchProducts(String query) {
        String term = normalize(query);
        return products.values().stream()
                .filter(product -> contains(product.getName(), term)
                        || contains(product.getCategory(), term)
                        || contains(product.getDescription(), term))
                .collect(Collectors.toList());
    }

    public Optional<RegularProduct> findProduct(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    public List<FlashSaleEvent> listFlashSaleEvents() {
        return events.values().stream()
                .sorted(Comparator.comparing(FlashSaleEvent::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<FlashSaleItem> listFlashSaleItems(String eventId) {
        return flashSaleItems.values().stream()
                .filter(item -> eventId == null || eventId.equals(item.getEventId()))
                .collect(Collectors.toList());
    }

    public List<FlashSaleItem> searchFlashSaleItems(String query) {
        String term = normalize(query);
        return flashSaleItems.values().stream()
                .filter(item -> contains(item.getProductName(), term)
                        || contains(item.getProductId(), term)
                        || contains(item.getId(), term)
                        || contains(item.getEventId(), term))
                .collect(Collectors.toList());
    }

    public List<FlashSaleEvent> searchFlashSales(String query) {
        String term = normalize(query);
        return events.values().stream()
                .filter(event -> contains(event.getEventId(), term)
                        || contains(event.getTitle(), term)
                        || contains(event.getDescription(), term))
                .collect(Collectors.toList());
    }

    public Optional<FlashSaleItem> findFlashSaleItem(String itemId) {
        return Optional.ofNullable(flashSaleItems.get(itemId));
    }

    public Cart viewCart(String customerId) {
        requireCustomer(customerId);
        return carts.computeIfAbsent(customerId, id -> new Cart("CART-" + id, id));
    }

    public Cart addToCart(String customerId, String productId, int quantity, boolean flashSale) {
        requireCustomer(customerId);
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        Cart cart = viewCart(customerId);
        String cartProductId;
        BigDecimal price;
        if (flashSale) {
            FlashSaleItem item = requireFlashSaleItem(productId);
            FlashSaleEvent event = requireEvent(item.getEventId());
            if (!event.isActive()) {
                throw new IllegalArgumentException("The Flash Sale event is not active.");
            }
            cartProductId = FLASH_ITEM_PREFIX + item.getId();
            int alreadyInCart = quantityInCart(cart, cartProductId);
            if (item.getStock() < alreadyInCart + quantity) {
                throw new IllegalArgumentException("Not enough Flash Sale stock.");
            }
            int alreadyOrdered = orders.values().stream()
                    .filter(order -> order.getCustomerId().equals(customerId)
                            && order.getStatus() != OrderStatus.CANCELLED)
                    .flatMap(order -> order.getDetails().stream())
                    .filter(detail -> detail.getProductId().equals(FLASH_ITEM_PREFIX + item.getId()))
                    .mapToInt(OrderDetail::getQuantity).sum();
            if (alreadyInCart + alreadyOrdered + quantity > item.getMaxPerUser()) {
                throw new IllegalArgumentException("The per-customer Flash Sale limit was exceeded.");
            }
            price = item.getSalePrice();
        } else {
            RegularProduct product = requireProduct(productId);
            cartProductId = product.getProductId();
            if (!product.isAvailable()
                    || product.getStock() < quantityInCart(cart, cartProductId) + quantity) {
                throw new IllegalArgumentException("Product is unavailable or stock is insufficient.");
            }
            price = product.getPrice();
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(cartProductId)).findFirst();
        if (existing.isPresent()) {
            cart.updateQuantity(existing.get().getCartItemId(),
                    existing.get().getQuantity() + quantity);
        } else {
            cart.addItem(new CartItem("CI-" + cartItemSequence++,
                    cart.getCartId(), cartProductId, quantity, price));
        }
        return cart;
    }

    public Order placeOrder(String customerId, String voucherCode) {
        Customer customer = requireCustomer(customerId);
        Cart cart = viewCart(customerId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("The cart is empty.");
        }
        BigDecimal subtotal = cart.calculateTotal();
        BigDecimal tierDiscount = subtotal.multiply(
                BigDecimal.valueOf(customer.getTier().getDiscountRate()));
        BigDecimal discountedSubtotal = subtotal.subtract(tierDiscount).max(BigDecimal.ZERO);
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher voucher = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            voucher = voucherRepository.findByCode(voucherCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Voucher code does not exist."));
            if (!voucher.isValid() || voucher.isUsed()) {
                throw new IllegalArgumentException("Voucher is not currently valid or has been used.");
            }
            if (discountedSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new IllegalArgumentException("Order does not meet the voucher minimum.");
            }
            voucherDiscount = voucher.calculateDiscount(discountedSubtotal);
        }

        String orderId = nextId("ORD-", orderSequence++);
        Order order = new Order(orderId, customerId, OrderStatus.PENDING);
        List<CartItem> reservedItems = new ArrayList<>();
        try {
            for (CartItem cartItem : cart.getItems()) {
                reserveStock(cartItem);
                reservedItems.add(cartItem);
                order.addDetail(new OrderDetail(orderId + "-D" + order.getDetails().size(),
                        orderId, cartItem.getProductId(), cartItem.getQuantity(),
                        cartItem.getUnitPrice()));
            }
        } catch (RuntimeException e) {
            restoreReservedStock(reservedItems);
            throw e;
        }
        order.setDiscountAmount(tierDiscount.add(voucherDiscount));
        orders.put(orderId, order);
        if (voucher != null) {
            orderVouchers.put(orderId, voucher.getCode());
        }
        for (CartItem cartItem : new ArrayList<>(cart.getItems())) {
            cart.removeItem(cartItem.getCartItemId());
        }
        return order;
    }

    public BigDecimal previewVoucher(String customerId, String voucherCode) {
        Customer customer = requireCustomer(customerId);
        BigDecimal subtotal = viewCart(customerId).calculateTotal();
        if (subtotal.signum() <= 0) {
            throw new IllegalArgumentException("Add products to the cart before applying a voucher.");
        }
        BigDecimal tierDiscount = subtotal.multiply(
                BigDecimal.valueOf(customer.getTier().getDiscountRate()));
        BigDecimal amountAfterTier = subtotal.subtract(tierDiscount).max(BigDecimal.ZERO);
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(() -> new IllegalArgumentException("Voucher code does not exist."));
        if (!voucher.isValid() || voucher.isUsed()) {
            throw new IllegalArgumentException("Voucher is not currently valid or has been used.");
        }
        if (amountAfterTier.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Order does not meet the voucher minimum.");
        }
        return amountAfterTier.subtract(voucher.calculateDiscount(amountAfterTier)).max(BigDecimal.ZERO);
    }

    public OrderTransaction makePayment(String customerId, String orderId, String method) {
        Customer customer = requireCustomer(customerId);
        Order order = requireOrder(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Order does not belong to this customer.");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.APPROVED) {
            throw new IllegalArgumentException("Order cannot be paid in its current state.");
        }
        String paymentMethod = normalize(method).toUpperCase(Locale.ROOT);
        if (!paymentMethod.equals("WALLET") && !paymentMethod.equals("VNPAY")) {
            throw new IllegalArgumentException("Payment method must be WALLET or VNPAY.");
        }
        BigDecimal amount = order.getFinalAmount();
        if (paymentMethod.equals("WALLET")) {
            BigDecimal balance = BigDecimal.valueOf(customer.getWalletBalance());
            if (balance.compareTo(amount) < 0) {
                throw new IllegalArgumentException("Wallet balance is insufficient.");
            }
            customer.setWalletBalance(balance.subtract(amount).doubleValue());
            customerRepository.save(customer);
        }
        OrderTransaction transaction = new OrderTransaction(
                nextId("TX-", transactionSequence++), orderId, customerId, amount,
                paymentMethod, TransactionStatus.PENDING, LocalDateTime.now());
        if (!transaction.processPayment()) {
            throw new IllegalStateException("Payment could not be completed.");
        }
        transactions.put(transaction.getId(), transaction);
        orderPaymentMethods.put(orderId, paymentMethod);
        order.setStatus(OrderStatus.PAID);
        useVoucherForOrder(orderId);
        return transaction;
    }

    public List<Order> viewOrderHistory(String customerId) {
        requireCustomer(customerId);
        return orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<OrderTransaction> viewTransactionHistory(String customerId) {
        requireCustomer(customerId);
        return transactions.values().stream()
                .filter(transaction -> transaction.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(OrderTransaction::getTransactionTime).reversed())
                .collect(Collectors.toList());
    }

    public void cancelCustomerOrder(String customerId, String orderId) {
        Order order = requireOrder(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Order does not belong to this customer.");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.APPROVED) {
            throw new IllegalArgumentException("Only unpaid orders can be cancelled by a customer.");
        }
        cancelOrder(order, false);
    }

    public List<Customer> viewUsers() {
        return adminCustomerController.viewUsers();
    }

    public Customer createUser(String name, String email, String phone, String address, String password) {
        return adminCustomerController.createUser(name, email, phone, address, password);
    }

    public Customer updateUser(String customerId, String name, String phone, String address) {
        return adminCustomerController.updateUser(customerId, name, phone, address);
    }

    public boolean deleteUser(String customerId) {
        boolean hasLiveOrders = orders.values().stream()
                .anyMatch(order -> order.getCustomerId().equals(customerId)
                        && order.getStatus() != OrderStatus.CANCELLED);
        if (hasLiveOrders) {
            throw new IllegalArgumentException("Cannot delete a customer with active orders.");
        }
        return adminCustomerController.deleteUser(customerId);
    }

    public Customer setUserStatus(String customerId, CustomerStatus status) {
        return adminCustomerController.updateStatus(customerId, status);
    }

    public RegularProduct createProduct(String name, String description, String category,
            BigDecimal price, int stock) {
        String id = nextId("P-", productSequence++);
        RegularProduct product = new RegularProduct(id, requireText(name, "Product name"),
                description == null ? "" : description.trim(), requireText(category, "Category"),
                price, stock, 0, "ACTIVE");
        products.put(id, product);
        return product;
    }

    public RegularProduct updateProduct(String productId, String name, String description,
            String category, BigDecimal price, String status) {
        RegularProduct product = requireProduct(productId);
        product.setName(requireText(name, "Product name"));
        product.setDescription(description == null ? "" : description.trim());
        product.setCategory(requireText(category, "Category"));
        product.setPrice(price);
        product.setStatus(status);
        product.setVersion(product.getVersion() + 1);
        return product;
    }

    public boolean deleteProduct(String productId) {
        boolean belongsToSale = flashSaleItems.values().stream()
                .anyMatch(item -> productId.equals(item.getProductId()));
        boolean belongsToOrder = orders.values().stream().flatMap(order -> order.getDetails().stream())
                .anyMatch(detail -> productId.equals(detail.getProductId()));
        if (belongsToSale || belongsToOrder) {
            throw new IllegalArgumentException("Cannot delete a product referenced by a sale or order.");
        }
        return products.remove(productId) != null;
    }

    public int changeInventory(String productId, int adjustment) {
        if (adjustment == 0) {
            throw new IllegalArgumentException("Stock adjustment cannot be zero.");
        }
        if (productId.startsWith(FLASH_ITEM_PREFIX)) {
            FlashSaleItem item = requireFlashSaleItem(productId.substring(FLASH_ITEM_PREFIX.length()));
            long newStock = (long) item.getStock() + adjustment;
            if (newStock < 0) {
                throw new IllegalArgumentException("Stock cannot be reduced below zero.");
            }
            if (newStock > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Stock adjustment exceeds the supported limit.");
            }
            item.setStock((int) newStock);
            return item.getStock();
        }
        RegularProduct product = requireProduct(productId);
        long newStock = (long) product.getStock() + adjustment;
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be reduced below zero.");
        }
        if (newStock > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Stock adjustment exceeds the supported limit.");
        }
        product.setStock((int) newStock);
        product.setVersion(product.getVersion() + 1);
        return product.getStock();
    }

    public FlashSaleEvent createFlashSale(String title, String description,
            LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Flash Sale requires a valid start and end time.");
        }
        String id = nextId("EVT-", eventSequence++);
        LocalDateTime now = LocalDateTime.now();
        SaleStatus status = now.isBefore(start) ? SaleStatus.UPCOMING
                : now.isBefore(end) ? SaleStatus.ACTIVE : SaleStatus.ENDED;
        FlashSaleEvent event = new FlashSaleEvent(id, requireText(title, "Event title"),
                description == null ? "" : description.trim(), start, end, status);
        events.put(id, event);
        return event;
    }

    public FlashSaleEvent updateFlashSale(String eventId, String title, String description,
            LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Flash Sale requires a valid start and end time.");
        }
        FlashSaleEvent old = requireEvent(eventId);
        FlashSaleEvent updated = new FlashSaleEvent(old.getEventId(),
                requireText(title, "Event title"), description == null ? "" : description.trim(),
                start, end, old.getStatus());
        events.put(eventId, updated);
        return updated;
    }

    public void endFlashSale(String eventId) {
        FlashSaleEvent event = requireEvent(eventId);
        event.setStatus(SaleStatus.ENDED);
        releaseUnsoldFlashSaleStock(eventId);
    }

    public void cancelFlashSale(String eventId) {
        FlashSaleEvent event = requireEvent(eventId);
        event.setStatus(SaleStatus.INACTIVE);
        releaseUnsoldFlashSaleStock(eventId);
    }

    private void releaseUnsoldFlashSaleStock(String eventId) {
        for (FlashSaleItem item : flashSaleItems.values()) {
            if (!eventId.equals(item.getEventId()) || item.getStock() == 0) {
                continue;
            }
            RegularProduct product = products.get(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getStock());
            }
            item.setStock(0);
        }
    }

    public FlashSaleItem addFlashSaleProduct(String eventId, String productId,
            BigDecimal salePrice, int quantity, int maxPerUser) {
        FlashSaleEvent event = requireEvent(eventId);
        if (event.getStatus() == SaleStatus.ENDED || event.getStatus() == SaleStatus.INACTIVE
                || event.getStatus() == SaleStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot add products to an ended or cancelled event.");
        }
        RegularProduct product = requireProduct(productId);
        if (flashSaleItems.values().stream().anyMatch(item -> eventId.equals(item.getEventId())
                && productId.equals(item.getProductId()))) {
            throw new IllegalArgumentException("Product is already assigned to this Flash Sale.");
        }
        if (quantity < 1 || quantity > product.getStock()) {
            throw new IllegalArgumentException("Flash Sale quantity must be within regular inventory.");
        }
        if (salePrice.compareTo(product.getPrice()) > 0) {
            throw new IllegalArgumentException("Flash Sale price cannot exceed regular price.");
        }
        String id = nextId("FS-", flashItemSequence++);
        FlashSaleItem item = new FlashSaleItem(id, event.getEventId(), productId,
                product.getPrice(), salePrice, quantity, 0, maxPerUser, 0);
        item.setProductName(product.getName());
        product.setStock(product.getStock() - quantity);
        flashSaleItems.put(id, item);
        return item;
    }

    public Voucher createVoucher(String code, String type, BigDecimal amount, BigDecimal minOrder,
            BigDecimal maxDiscount, LocalDateTime validFrom, LocalDateTime validTo) {
        if (voucherRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Voucher code already exists.");
        }
        String id = "V-" + (voucherRepository.getAll().size() + 1);
        while (voucherRepository.findById(id).isPresent()) {
            id = "V-" + (Integer.parseInt(id.substring(2)) + 1);
        }
        Voucher voucher = new Voucher(id, requireText(code, "Voucher code").toUpperCase(Locale.ROOT),
                type, amount, minOrder, maxDiscount, validFrom, validTo, VoucherStatus.ACTIVE);
        List<Voucher> vouchers = new ArrayList<>(voucherRepository.getAll());
        vouchers.add(voucher);
        voucherRepository.saveAll(vouchers);
        return voucher;
    }

    public List<Voucher> listVouchers() {
        return voucherRepository.getAll();
    }

    public Voucher updateVoucher(String voucherId, String code, String type, BigDecimal amount,
            BigDecimal minOrder, BigDecimal maxDiscount, LocalDateTime validFrom,
            LocalDateTime validTo, VoucherStatus status) {
        List<Voucher> vouchers = new ArrayList<>(voucherRepository.getAll());
        Voucher voucher = vouchers.stream().filter(item -> item.getVoucherId().equals(voucherId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Voucher does not exist."));
        String normalizedCode = requireText(code, "Voucher code").toUpperCase(Locale.ROOT);
        if (vouchers.stream().anyMatch(item -> !item.getVoucherId().equals(voucherId)
                && item.getCode().equalsIgnoreCase(normalizedCode))) {
            throw new IllegalArgumentException("Voucher code already exists.");
        }
        voucher.setValidTo(null);
        voucher.setCode(normalizedCode);
        voucher.setDiscountType(type);
        voucher.setDiscountAmount(amount);
        voucher.setMinOrderValue(minOrder);
        voucher.setMaxDiscountValue(maxDiscount);
        voucher.setValidFrom(validFrom);
        voucher.setValidTo(validTo);
        voucher.setStatus(status);
        voucherRepository.saveAll(vouchers);
        return voucher;
    }

    public boolean deleteVoucher(String voucherId) {
        List<Voucher> vouchers = new ArrayList<>(voucherRepository.getAll());
        boolean removed = vouchers.removeIf(voucher -> voucher.getVoucherId().equals(voucherId));
        if (removed) {
            voucherRepository.saveAll(vouchers);
        }
        return removed;
    }

    public List<Order> listOrders() {
        return new ArrayList<>(orders.values());
    }

    public Order approveOrder(String orderId) {
        Order order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Order cannot be approved in its current state.");
        }
        order.setStatus(OrderStatus.APPROVED);
        return order;
    }

    public void cancelOrderAsAdmin(String orderId) {
        cancelOrder(requireOrder(orderId), true);
    }

    public Map<String, BigDecimal> salesReport() {
        Map<String, BigDecimal> report = new LinkedHashMap<>();
        report.put("successfulTransactions", transactions.values().stream()
                .filter(OrderTransaction::isSuccess)
                .filter(transaction -> {
                    Order order = orders.get(transaction.getOrderId());
                    return order != null && order.getStatus() != OrderStatus.CANCELLED;
                })
                .map(OrderTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        report.put("successfulOrderCount", BigDecimal.valueOf(orders.values().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID
                        || order.getStatus() == OrderStatus.APPROVED
                        || order.getStatus() == OrderStatus.SUCCESS).count()));
        report.put("allOrders", BigDecimal.valueOf(orders.size()));
        return report;
    }

    public void exportTransactions(Path outputFile) throws IOException {
        Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>();
        lines.add("transactionId,orderId,customerId,amount,paymentMethod,status,transactionTime");
        for (OrderTransaction transaction : transactions.values()) {
            lines.add(transaction.toCsvLine());
        }
        Files.write(outputFile, lines, StandardCharsets.UTF_8);
    }

    private void cancelOrder(Order order, boolean admin) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        boolean paid = order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.APPROVED
                || order.getStatus() == OrderStatus.SUCCESS;
        if (paid && !admin) {
            throw new IllegalArgumentException("A paid order requires administrator cancellation.");
        }
        if (paid && "WALLET".equals(orderPaymentMethods.get(order.getOrderId()))) {
            Customer customer = requireCustomer(order.getCustomerId());
            customer.setWalletBalance(customer.getWalletBalance()
                    + order.getFinalAmount().doubleValue());
            customerRepository.save(customer);
        }
        for (OrderDetail detail : order.getDetails()) {
            restoreStock(detail.getProductId(), detail.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    public List<OrderTransaction> viewAllTransactions() {
        return new ArrayList<>(transactions.values());
    }

    private void reserveStock(CartItem item) {
        if (item.getProductId().startsWith(FLASH_ITEM_PREFIX)) {
            FlashSaleItem flashItem = requireFlashSaleItem(
                    item.getProductId().substring(FLASH_ITEM_PREFIX.length()));
            if (!requireEvent(flashItem.getEventId()).isActive()) {
                throw new IllegalArgumentException("The Flash Sale event has ended.");
            }
            if (!flashItem.sell(item.getQuantity())) {
                throw new IllegalArgumentException("Flash Sale stock or per-user limit was exceeded.");
            }
        } else {
            RegularProduct product = requireProduct(item.getProductId());
            if (!product.isAvailable() || product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Product stock is no longer available.");
            }
            product.setStock(product.getStock() - item.getQuantity());
        }
    }

    private void restoreReservedStock(List<CartItem> items) {
        for (CartItem item : items) {
            restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    private void restoreStock(String productId, int quantity) {
        if (productId.startsWith(FLASH_ITEM_PREFIX)) {
            FlashSaleItem item = requireFlashSaleItem(productId.substring(FLASH_ITEM_PREFIX.length()));
            item.setSoldQuantity(Math.max(0, item.getSoldQuantity() - quantity));
            FlashSaleEvent event = events.get(item.getEventId());
            if (event != null && (event.getStatus() == SaleStatus.ENDED
                    || event.getStatus() == SaleStatus.INACTIVE
                    || event.getStatus() == SaleStatus.EXPIRED)) {
                RegularProduct product = requireProduct(item.getProductId());
                product.setStock(product.getStock() + quantity);
            } else {
                item.setStock(item.getStock() + quantity);
            }
        } else {
            RegularProduct product = requireProduct(productId);
            product.setStock(product.getStock() + quantity);
        }
    }

    private void useVoucherForOrder(String orderId) {
        String code = orderVouchers.get(orderId);
        if (code == null) {
            return;
        }
        List<Voucher> vouchers = new ArrayList<>(voucherRepository.getAll());
        for (Voucher voucher : vouchers) {
            if (voucher.getCode().equalsIgnoreCase(code)) {
                voucher.setStatus(VoucherStatus.USED);
                voucherRepository.saveAll(vouchers);
                return;
            }
        }
    }

    private Customer requireCustomer(String customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer is required.");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer does not exist."));
    }

    private RegularProduct requireProduct(String productId) {
        RegularProduct product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product does not exist: " + productId);
        }
        return product;
    }

    private FlashSaleItem requireFlashSaleItem(String itemId) {
        FlashSaleItem item = flashSaleItems.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Flash Sale product does not exist: " + itemId);
        }
        return item;
    }

    private FlashSaleEvent requireEvent(String eventId) {
        FlashSaleEvent event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Flash Sale event does not exist: " + eventId);
        }
        return event;
    }

    private Order requireOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order does not exist: " + orderId);
        }
        return order;
    }

    private void seedCatalog() {
        addSeedProduct("P-001", "Wireless Headphones", "Bluetooth noise-cancelling headphones",
                "Electronics", "129.99", 30);
        addSeedProduct("P-002", "Coffee Maker", "Compact programmable coffee maker",
                "Home", "79.50", 20);
        addSeedProduct("P-003", "Running Shoes", "Lightweight everyday running shoes",
                "Sports", "95.00", 25);
        LocalDateTime now = LocalDateTime.now();
        FlashSaleEvent event = new FlashSaleEvent("EVT-001", "Welcome Flash Sale",
                "Sample event; update its schedule in the admin menu.",
                now.minusHours(1), now.plusDays(30), SaleStatus.ACTIVE);
        events.put(event.getEventId(), event);
        FlashSaleItem flashItem = new FlashSaleItem("FS-001", event.getEventId(), "P-001",
                new BigDecimal("129.99"), new BigDecimal("99.99"), 10, 0, 2, 0);
        flashItem.setProductName("Wireless Headphones");
        flashSaleItems.put(flashItem.getId(), flashItem);
    }

    private void addSeedProduct(String id, String name, String description, String category,
            String price, int stock) {
        products.put(id, new RegularProduct(id, name, description, category,
                new BigDecimal(price), stock, 0, "ACTIVE"));
    }

    private static String nextId(String prefix, int sequence) {
        return prefix + String.format(Locale.ROOT, "%04d", sequence);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}

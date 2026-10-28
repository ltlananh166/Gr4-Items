package repository;

import controller.SimulatorController;
import model.Customer;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.RegularProduct;
import model.SimulationResult;
import model.Voucher;
import model.enums.OrderStatus;
import model.enums.LockMechanism;
import model.enums.TransactionStatus;
import service.DataNormalizationService;
import service.CustomerService;
import service.DataGeneratorService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class CsvDataIntegrationVerification {
    public static void main(String[] args) throws Exception {
        verifyExistingDataLoads();
        verifyProductBrandPersists();
        verifyFlashSaleStockPersists();
        verifyVoucherUsagePersists();
        verifyOrderDataRoundTrips();
        verifyDataNormalization();
        verifyGeneratedDatasetRelations();
        System.out.println("ALL CSV DATA INTEGRATION CHECKS PASSED");
    }

    private static void verifyProductBrandPersists() throws Exception {
        Path directory = Files.createTempDirectory("product-csv-");
        ProductRepository products = new ProductRepository(directory.toString());
        RegularProduct product = new RegularProduct(
                "P-TEST", "Test product", "Test", "Accessories",
                new java.math.BigDecimal("125.50"), 4, 0, "ACTIVE");
        product.setBrand("Example");
        products.save(product);

        RegularProduct reloaded = new ProductRepository(directory.toString())
                .findById("P-TEST");
        check(reloaded != null && "Example".equals(reloaded.getBrand()),
                "Persist and reload product brand");
    }

    private static void verifyExistingDataLoads() {
        ProductRepository products = new ProductRepository("data");
        check(products.findAll().size() == 5_000, "Load all products");
        RegularProduct product = products.findById("P-00001");
        check(product != null && product.getBrand() != null
                && !product.getBrand().isBlank(), "Load product brand");

        FlashSaleItemRepository items = new FlashSaleItemRepository("data");
        check(items.findAll().size() == 500, "Load all flash sale items");
        check(items.findById("FI-00001") != null, "Find flash sale item by CSV ID");

        FlashSaleEventRepository events = new FlashSaleEventRepository("data");
        check(events.findAll().size() == 10, "Load all flash sale events");
        FlashSaleEvent event = events.findById("EV-001");
        check(event != null && event.getTitle().equals("Flash Sale 01"),
                "Map flash event name");
        check("EV-011".equals(events.generateNewEventId()),
                "Continue event IDs from CSV data");

        CustomerRepository customers = new CustomerRepository("data/customers.csv");
        var allCustomers = customers.getAll();
        check(allCustomers.size() >= 2_000, "Load customer CSV");

        VoucherRepository vouchers = new VoucherRepository("data/vouchers.csv");
        check(vouchers.getAll().size() == 10, "Load voucher CSV");
        check(vouchers.findByCode("SHOPEE10").orElseThrow().getRemainingUses() == 1_000,
                "Load voucher remaining uses");

        OrderRepository orders = new OrderRepository("data");
        var allOrders = orders.findAll();
        check(allOrders.size() == 2_500, "Load orders CSV");
        check(orders.findById("O-00001").getCustomerName() != null,
                "Map order customer name");

        OrderDetailRepository details = new OrderDetailRepository("data");
        var allDetails = details.findAll();
        check(allDetails.size() == 2_500, "Load order details CSV");
        check(details.findById("D-000001").calculateSubtotal()
                .compareTo(new BigDecimal("2406000")) == 0,
                "Map order detail ID and subtotal");

        OrderTransactionRepository transactions = new OrderTransactionRepository("data");
        check(transactions.findAll().isEmpty(), "Load empty transactions CSV");

        Set<String> productIds = products.findAll().stream()
                .map(RegularProduct::getProductId).collect(Collectors.toSet());
        Set<String> eventIds = events.findAll().stream()
                .map(FlashSaleEvent::getEventId).collect(Collectors.toSet());
        Set<String> customerIds = allCustomers.stream()
                .map(Customer::getCustomerId).collect(Collectors.toSet());
        Set<String> orderIds = allOrders.stream().map(Order::getOrderId)
                .collect(Collectors.toSet());
        check(items.findAll().stream().allMatch(item -> productIds.contains(item.getProductId())
                && eventIds.contains(item.getEventId())), "Validate flash item references");
        check(allOrders.stream().allMatch(order -> customerIds.contains(order.getCustomerId())),
                "Validate order customer references");
        check(allDetails.stream().allMatch(detail -> orderIds.contains(detail.getOrderId())
                && productIds.contains(detail.getProductId())),
                "Validate order detail references");
    }

    private static void verifyVoucherUsagePersists() throws Exception {
        Path directory = Files.createTempDirectory("voucher-csv-");
        Path file = directory.resolve("vouchers.csv");
        Files.writeString(file, "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses\n"
                + "V-TEST,TEST10,PERCENTAGE,10,500,0,3\n");
        VoucherRepository vouchers = new VoucherRepository(file.toString());
        Voucher voucher = vouchers.findByCode("TEST10").orElseThrow();
        check(voucher.consumeUse(), "Consume available voucher use");
        vouchers.update(voucher);
        Voucher reloaded = new VoucherRepository(file.toString())
                .findByCode("TEST10").orElseThrow();
        check(reloaded.getRemainingUses() == 2, "Persist voucher remaining uses");
        check(reloaded.getMaxDiscountValue().compareTo(new BigDecimal("500")) == 0
                && reloaded.getMinOrderValue().signum() == 0,
                "Preserve voucher value column order");
        CustomerService customerService = new CustomerService(
                new CustomerRepository(directory.resolve("customers.csv").toString()), vouchers);
        double finalAmount = customerService.applyVoucher(
                new Customer("C-TEST", "Test", "test@example.com", "", "", ""),
                "TEST10", 1_000);
        check(finalAmount == 900.0, "Apply voucher through customer service");
        check(vouchers.findByCode("TEST10").orElseThrow().getRemainingUses() == 1,
                "Deduct use when customer applies voucher");
        check(Files.readAllLines(file).get(0).equals(
                "voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses"),
                "Preserve voucher CSV header");
    }

    private static void verifyOrderDataRoundTrips() throws Exception {
        Path directory = Files.createTempDirectory("order-csv-");
        Files.writeString(directory.resolve("orders.csv"),
                "orderId,customerId,customerName,orderDate,totalAmount,status,eventId\n");
        Files.writeString(directory.resolve("order_details.csv"),
                "detailId,orderId,productId,quantity,unitPrice,subtotal\n");
        Files.writeString(directory.resolve("transactions.csv"),
                "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp\n");
        OrderRepository orders = new OrderRepository(directory.toString());
        Order order = new Order("O-TEST", "C-TEST", OrderStatus.SUCCESS);
        order.setCustomerName("Test Customer");
        order.setOrderDate("2026-10-28");
        order.setEventId("EV-001");
        order.setTotalAmount(new BigDecimal("200"));
        orders.save(order);

        OrderDetailRepository details = new OrderDetailRepository(directory.toString());
        details.save(new OrderDetail("D-TEST", "O-TEST", "P-00001", 2,
                new BigDecimal("100")));

        OrderTransactionRepository transactions =
                new OrderTransactionRepository(directory.toString());
        OrderTransaction transaction = new OrderTransaction("T-TEST", "O-TEST", "C-TEST",
                BigDecimal.ZERO, null, TransactionStatus.SUCCESS, LocalDateTime.now());
        transaction.setItemId("FI-00001");
        transaction.setQuantity(2);
        transaction.setMessage("Test transaction");
        transactions.save(transaction);

        Order loadedOrder = new OrderRepository(directory.toString()).findById("O-TEST");
        check(loadedOrder != null && loadedOrder.getCustomerName().equals("Test Customer"),
                "Persist order with data CSV columns");
        OrderDetail loadedDetail = new OrderDetailRepository(directory.toString())
                .findById("D-TEST");
        check(loadedDetail != null && loadedDetail.calculateSubtotal()
                .compareTo(new BigDecimal("200")) == 0, "Persist order detail subtotal");
        OrderTransaction loadedTransaction =
                new OrderTransactionRepository(directory.toString()).findById("T-TEST");
        check(loadedTransaction != null && loadedTransaction.getQuantity() == 2
                && "FI-00001".equals(loadedTransaction.getItemId()),
                "Persist transaction CSV columns");
    }

    private static void verifyDataNormalization() throws Exception {
        Path directory = Files.createTempDirectory("data-normalization-");
        Files.writeString(directory.resolve("products.csv"),
                "productId,name,brand,category,price,stock,description,version\n"
                        + "P-TEST,Linked Product,Brand,Category,1000,4,Description,0\n");
        Files.writeString(directory.resolve("flash_items.csv"),
                "itemId,productId,eventId,productName,originalPrice,salePrice,"
                        + "initialStock,soldQty,remainingStock,version\n"
                        + "FI-TEST,P-TEST,EV-001,Wrong Name,2000,1000,5,1,4,2\n");
        Files.writeString(directory.resolve("orders.csv"),
                "orderId,customerId,customerName,orderDate,totalAmount,status,eventId\n"
                        + "O-TEST,C-TEST,Customer,2026-10-28,999,SUCCESS,EV-001\n");
        Files.writeString(directory.resolve("order_details.csv"),
                "detailId,orderId,productId,quantity,unitPrice,subtotal\n"
                        + "D-TEST,O-TEST,P-TEST,2,100,200\n");

        new DataNormalizationService(directory.toString()).normalizeRelations();

        FlashSaleItem item = new FlashSaleItemRepository(directory.toString())
                .findById("FI-TEST");
        check("Linked Product".equals(item.getProductName())
                && item.getOriginalPrice().compareTo(new BigDecimal("1000")) == 0
                && item.getSalePrice().compareTo(new BigDecimal("500")) == 0,
                "Normalize flash item product fields and preserve discount ratio");
        check(item.getInitialStock() == 5 && item.getSoldQty() == 1
                && item.getRemainingStock() == 4 && item.getVersion() == 2,
                "Preserve flash item inventory state while normalizing");
        Order order = new OrderRepository(directory.toString()).findById("O-TEST");
        check(order.getTotalAmount().compareTo(new BigDecimal("200")) == 0,
                "Normalize order total from detail rows");
    }

    private static void verifyGeneratedDatasetRelations() throws Exception {
        Path directory = Files.createTempDirectory("generated-data-");
        new DataGeneratorService(directory.toString()).generateAll();

        ProductRepository products = new ProductRepository(directory.toString());
        var productsById = products.findAll().stream()
                .collect(Collectors.toMap(RegularProduct::getProductId, product -> product));
        FlashSaleItemRepository items =
                new FlashSaleItemRepository(directory.toString());
        check(items.findAll().stream().allMatch(item -> {
            RegularProduct product = productsById.get(item.getProductId());
            return product != null && item.getProductName().equals(product.getName())
                    && item.getOriginalPrice().compareTo(product.getPrice()) == 0
                    && item.getSalePrice().compareTo(item.getOriginalPrice()) < 0;
        }), "Generate Flash Sale items linked to catalog products");

        OrderRepository orders = new OrderRepository(directory.toString());
        OrderDetailRepository details = new OrderDetailRepository(directory.toString());
        var totalsByOrderId = details.findAll().stream().collect(Collectors.groupingBy(
                OrderDetail::getOrderId,
                Collectors.reducing(BigDecimal.ZERO, OrderDetail::calculateSubtotal,
                        BigDecimal::add)));
        check(orders.findAll().stream().allMatch(order ->
                order.getTotalAmount().compareTo(totalsByOrderId.get(order.getOrderId())) == 0),
                "Generate order totals from order details");
    }

    private static void verifyFlashSaleStockPersists() throws Exception {
        Path directory = Files.createTempDirectory("flash-item-csv-");
        Path csv = directory.resolve("flash_items.csv");
        Files.writeString(csv, "itemId,productId,eventId,productName,originalPrice,salePrice,"
                + "initialStock,soldQty,remainingStock,version\n"
                + "FI-TEST,P-00001,EV-001,Test item,100,50,5,0,5,0\n");

        FlashSaleItemRepository repository =
                new FlashSaleItemRepository(directory.toString());
        check(repository.sellWithSynchronized("FI-TEST", 2),
                "Sell against CSV-backed stock");
        repository.persist();

        try (SimulatorController controller = new SimulatorController(
                repository, new SimulationRecordRepository())) {
            SimulationResult result = controller.runSimulation(
                    "FI-TEST", 8, 1, LockMechanism.SYNCHRONIZED);
            check(result.getSuccessCount() == 3 && result.getFailedCount() == 5,
                    "Simulate requests against remaining CSV stock");
        }

        FlashSaleItem reloaded = new FlashSaleItemRepository(directory.toString())
                .findById("FI-TEST");
        check(reloaded != null && reloaded.getRemainingStock() == 0
                && reloaded.getSoldQty() == 5, "Persist and reload simulation stock");

        Path noLockDirectory = Files.createTempDirectory("flash-item-race-");
        Files.writeString(noLockDirectory.resolve("flash_items.csv"),
                "itemId,productId,eventId,productName,originalPrice,salePrice,"
                        + "initialStock,soldQty,remainingStock,version\n"
                        + "FI-RACE,P-00001,EV-001,Race item,100,50,1,0,1,0\n");
        FlashSaleItemRepository noLockItems =
                new FlashSaleItemRepository(noLockDirectory.toString());
        try (SimulatorController controller = new SimulatorController(
                noLockItems, new SimulationRecordRepository())) {
            controller.runSimulation("FI-RACE", 20, 1, LockMechanism.NO_LOCK);
        }
        FlashSaleItem noLockItem = new FlashSaleItemRepository(noLockDirectory.toString())
                .findById("FI-RACE");
        check(noLockItem.getInitialStock()
                == noLockItem.getRemainingStock() + noLockItem.getSoldQty(),
                "Keep CSV stock counts consistent after no-lock simulation");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        System.out.println("PASSED: " + name);
    }
}


package repository;

import model.RegularProduct;
import model.OrderDetail;
import model.OrderTransaction;
import model.FlashSaleEvent;
import model.enums.TransactionStatus;
import model.enums.SaleStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class TV1RepositoryVerification {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("tv1-repository-test-");
        String dataDir = dir.toString();

        // 1. ProductRepository
        ProductRepository products = new ProductRepository(dataDir);
        RegularProduct product = new RegularProduct(
                "P-00001", "Keyboard", "Mechanical keyboard",
                "Electronics", new BigDecimal("1500000"),
                10, 0, "ACTIVE");
        products.save(product);

        check(products.findAll().size() == 1, "Product save/read");
        check(products.findById("P-00001") != null, "Product findById");
        check(products.findAvailableProducts().size() == 1,
                "Product availability");
        check("P-00002".equals(products.generateNewProductId()),
                "Product ID generation");

        // 2. OrderDetailRepository
        OrderDetailRepository details = new OrderDetailRepository(dataDir);
        OrderDetail detail = new OrderDetail(
                "OD-00001", "O-00001", "P-00001",
                2, new BigDecimal("1500000"));
        details.save(detail);

        check(details.findAll().size() == 1, "OrderDetail save/read");
        check(details.findById("OD-00001") != null,
                "OrderDetail findById");
        check(details.findByOrderId("O-00001").size() == 1,
                "OrderDetail findByOrderId");

        // 3. OrderTransactionRepository
        OrderTransactionRepository transactions =
                new OrderTransactionRepository(dataDir);

        OrderTransaction transaction = new OrderTransaction(
                "T-00001", "O-00001", "C-00001",
                new BigDecimal("3000000"), "BANK_TRANSFER",
                TransactionStatus.PENDING, LocalDateTime.now());
        transactions.save(transaction);

        check(transactions.findAll().size() == 1,
                "Transaction save/read");
        check(transactions.findById("T-00001") != null,
                "Transaction findById");
        check(transactions.findByOrderId("O-00001").size() == 1,
                "Transaction findByOrderId");

        // 4. FlashSaleEventRepository
        FlashSaleEventRepository events =
                new FlashSaleEventRepository(dataDir);

        FlashSaleEvent event = new FlashSaleEvent(
                "E-00001", "Flash Sale", "Test event",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                SaleStatus.ACTIVE);
        events.save(event);

        check(events.findAll().size() == 1, "Event save/read");
        check(events.findById("E-00001") != null, "Event findById");
        check(events.findActiveEvents().size() == 1,
                "Event findActiveEvents");

        System.out.println("ALL TV1 REPOSITORY TESTS PASSED");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        System.out.println("PASSED: " + name);
    }
}

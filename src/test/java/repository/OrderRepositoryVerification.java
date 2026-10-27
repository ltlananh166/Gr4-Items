
package repository;

import model.Order;
import model.enums.OrderStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class OrderRepositoryVerification {

    public static void main(String[] args) throws Exception {

        Path testDirectory = Files.createTempDirectory(
                "order-repository-test-");

        try {
            OrderRepository repository =
                    new OrderRepository(testDirectory.toString());

            Order order = new Order(
                    "O-00001",
                    "C-00001",
                    OrderStatus.PENDING
            );

            order.setTotalAmount(new BigDecimal("500000"));
            order.setDiscountAmount(new BigDecimal("50000"));

            repository.save(order);

            List<Order> orders = repository.findAll();

            if (orders.size() != 1) {
                throw new AssertionError(
                        "Expected 1 order, found " + orders.size());
            }

            Order loaded = orders.get(0);

            check("O-00001".equals(loaded.getOrderId()),
                    "Order ID mismatch");

            check("C-00001".equals(loaded.getCustomerId()),
                    "Customer ID mismatch");

            check(loaded.getStatus() == OrderStatus.PENDING,
                    "Order status mismatch");

            check(loaded.getTotalAmount().compareTo(
                    new BigDecimal("500000")) == 0,
                    "Total amount mismatch");

            check(loaded.getDiscountAmount().compareTo(
                    new BigDecimal("50000")) == 0,
                    "Discount amount mismatch");

            check(loaded.getFinalAmount().compareTo(
                    new BigDecimal("450000")) == 0,
                    "Final amount mismatch");

            check(repository.findById("O-00001") != null,
                    "findById failed");

            check(repository.findByCustomerId("C-00001").size() == 1,
                    "findByCustomerId failed");

            check("O-00002".equals(repository.generateNewOrderId()),
                    "generateNewOrderId failed");

            System.out.println("ALL ORDER REPOSITORY TESTS PASSED");

        } finally {
            try (Stream<Path> paths = Files.walk(testDirectory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

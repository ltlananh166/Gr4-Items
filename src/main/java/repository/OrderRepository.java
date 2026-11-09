
package repository;

import model.Order;
import model.enums.OrderStatus;
import java.util.List;
import java.util.stream.Collectors;

public class OrderRepository extends CsvRepository<Order> {

    private static final String FILE_NAME = "orders.csv";

    private static final String HEADER =
            "orderId,customerId,totalAmount,discountAmount,"
            + "finalAmount,status,createdAt";

    public OrderRepository() {
        super(Order.class, FILE_NAME, HEADER);
    }

    public OrderRepository(String dataDirectory) {
        super(Order.class, dataDirectory, FILE_NAME, HEADER);
    }

    public String generateNewOrderId() {
        int maxId = findAll().stream()
                .map(Order::getOrderId)
                .filter(id -> id != null && id.matches("O-\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);

        return String.format("O-%d", maxId + 1);
    }

    public List<Order> findByCustomerId(String customerId) {
        return findAll().stream()
                .filter(order -> customerId != null
                        && customerId.equals(order.getCustomerId()))
                .collect(Collectors.toList());
    }

    public List<Order> findPurchasesCountingTowardLimit(String customerId, String eventId) {
        return findAll().stream()
                .filter(order -> customerId != null && customerId.equals(order.getCustomerId()))
                .filter(order -> eventId != null && eventId.equals(order.getEventId()))
                .filter(order -> order.getStatus() != null
                        && order.getStatus().countsTowardPurchaseLimit())
                .collect(Collectors.toList());
    }
}

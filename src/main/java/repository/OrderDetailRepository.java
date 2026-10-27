
package repository;

import model.OrderDetail;
import java.util.List;
import java.util.stream.Collectors;

public class OrderDetailRepository extends CsvRepository<OrderDetail> {

    private static final String FILE_NAME = "order_details.csv";

    private static final String HEADER =
            "orderDetailId,orderId,productId,quantity,unitPrice";

    public OrderDetailRepository() {
        super(OrderDetail.class, FILE_NAME, HEADER);
    }

    public OrderDetailRepository(String dataDirectory) {
        super(OrderDetail.class, dataDirectory, FILE_NAME, HEADER);
    }

    public String generateNewOrderDetailId() {
        int maxId = findAll().stream()
                .map(OrderDetail::getOrderDetailId)
                .filter(id -> id != null && id.matches("OD-\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(3)))
                .max()
                .orElse(0);

        return String.format("OD-%05d", maxId + 1);
    }

    public List<OrderDetail> findByOrderId(String orderId) {
        return findAll().stream()
                .filter(detail -> orderId != null
                        && orderId.equals(detail.getOrderId()))
                .collect(Collectors.toList());
    }
}


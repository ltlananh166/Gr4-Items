
package repository;

import model.OrderTransaction;
import java.util.List;
import java.util.stream.Collectors;

public class OrderTransactionRepository
        extends CsvRepository<OrderTransaction> {

    private static final String FILE_NAME = "transactions.csv";

    private static final String HEADER =
            "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp";

    public OrderTransactionRepository() {
        super(OrderTransaction.class, FILE_NAME, HEADER);
    }

    public OrderTransactionRepository(String dataDirectory) {
        super(OrderTransaction.class, dataDirectory, FILE_NAME, HEADER);
    }

    public String generateNewTransactionId() {
        int maxId = findAll().stream()
                .map(OrderTransaction::getId)
                .filter(id -> id != null && id.matches("T-\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);

        return String.format("T-%05d", maxId + 1);
    }

    public List<OrderTransaction> findByOrderId(String orderId) {
        return findAll().stream()
                .filter(transaction -> orderId != null
                        && orderId.equals(transaction.getOrderId()))
                .collect(Collectors.toList());
    }

    public List<OrderTransaction> findByCustomerId(String customerId) {
        return findAll().stream()
                .filter(transaction -> customerId != null
                        && customerId.equals(transaction.getCustomerId()))
                .collect(Collectors.toList());
    }
}

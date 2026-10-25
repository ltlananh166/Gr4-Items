
package repository;

import model.OrderTransaction;

public class OrderTransactionRepository extends CsvRepository<OrderTransaction> {

    public OrderTransactionRepository() {
        super(OrderTransaction.class, "order_transactions.csv",
                "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
    }

    public OrderTransactionRepository(String dataDirectory) {
        super(OrderTransaction.class, dataDirectory, "order_transactions.csv",
                "transactionId,orderId,customerId,itemId,quantity,status,message,timestamp");
    }
}

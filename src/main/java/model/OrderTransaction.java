package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import model.enums.TransactionStatus;

public class OrderTransaction extends BaseEntity {
    private String id;
    private String orderId;
    private String customerId;
    private String itemId;
    private int quantity;
    private String message;
    private BigDecimal amount = BigDecimal.ZERO;
    private String paymentMethod;
    private TransactionStatus status = TransactionStatus.PENDING;
    private LocalDateTime transactionTime;

    public OrderTransaction() {
        this.transactionTime = LocalDateTime.now();
    }

    public OrderTransaction(String id, String orderId, String customerId, BigDecimal amount,
            String paymentMethod, TransactionStatus status, LocalDateTime transactionTime) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        setAmount(amount);
        this.paymentMethod = paymentMethod;
        this.status = status == null ? TransactionStatus.PENDING : status;
        this.transactionTime = transactionTime == null ? LocalDateTime.now() : transactionTime;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(id), value(orderId), value(customerId), value(itemId),
                Integer.toString(quantity), status.name(), value(message), transactionTime.toString());
    }

    public boolean processPayment() {
        if (status != TransactionStatus.PENDING || amount.signum() <= 0
                || paymentMethod == null || paymentMethod.trim().isEmpty()) {
            status = TransactionStatus.FAILED;
            return false;
        }
        status = TransactionStatus.SUCCESS;
        transactionTime = LocalDateTime.now();
        return true;
    }

    public boolean isSuccess() { return status == TransactionStatus.SUCCESS; }

    private static String value(String value) { return value == null ? "" : value; }

    public void setId(String id) { this.id = id; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        this.quantity = quantity;
    }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null.");
        if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative.");
        this.amount = amount;
    }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) {
        this.status = status == null ? TransactionStatus.PENDING : status;
    }
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }
}

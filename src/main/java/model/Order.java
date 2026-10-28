package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import model.enums.OrderStatus;

public class Order extends BaseEntity {
    private String orderId;
    private String customerId;
    private String customerName;
    private String orderDate;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private OrderStatus status = OrderStatus.PENDING;
    private String eventId;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private final List<OrderDetail> details = new ArrayList<>();

    public Order() { }

    public Order(String orderId, String customerId, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status == null ? OrderStatus.PENDING : status;
    }

    @Override
    public String getId() { return orderId; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(orderId), value(customerId), value(customerName),
                value(orderDate), totalAmount.toPlainString(), status.name(), value(eventId));
    }

    public void addDetail(OrderDetail detail) {
        Objects.requireNonNull(detail, "Order detail cannot be null.");
        if (detail.getOrderId() != null && !detail.getOrderId().equals(orderId)) {
            throw new IllegalArgumentException("Order detail belongs to a different order.");
        }
        detail.setOrderId(orderId);
        details.add(detail);
        calculateTotal();
    }

    public BigDecimal calculateTotal() {
        if (!details.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (OrderDetail detail : details) total = total.add(detail.calculateSubtotal());
            totalAmount = total;
        }
        updateFinalAmount();
        return totalAmount;
    }

    public BigDecimal getFinalAmount() {
        calculateTotal();
        return finalAmount;
    }

    public void cancelOrder() {
        if (status == OrderStatus.PAID || status == OrderStatus.SUCCESS) {
            throw new IllegalStateException("A paid order cannot be cancelled.");
        }
        if (status == OrderStatus.CANCELLED) return;
        status = OrderStatus.CANCELLED;
    }

    private void updateFinalAmount() {
        finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.signum() < 0) finalAmount = BigDecimal.ZERO;
    }

    private static String value(String value) { return value == null ? "" : value; }

    public String getOrderId() { return orderId; }
    @Override
    public void setId(String id) { this.orderId = id; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BigDecimal getTotalAmount() { return calculateTotal(); }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = nonNegative(totalAmount, "Total amount");
        updateFinalAmount();
    }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = nonNegative(discountAmount, "Discount amount");
        updateFinalAmount();
    }
    public BigDecimal getFinalAmountValue() { return getFinalAmount(); }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status == null ? OrderStatus.PENDING : status; }
    public List<OrderDetail> getDetails() { return Collections.unmodifiableList(details); }
    public List<OrderDetail> getOrderDetails() { return getDetails(); }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " cannot be null.");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " cannot be negative.");
        return value;
    }
}

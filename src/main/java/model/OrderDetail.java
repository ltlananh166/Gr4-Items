package model;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderDetail extends BaseEntity {
    private String orderDetailId;
    private String orderId;
    private String productId;
    private int quantity;
    private BigDecimal unitPrice = BigDecimal.ZERO;

    public OrderDetail() { }

    public OrderDetail(String orderDetailId, String orderId, String productId,
            int quantity, BigDecimal unitPrice) {
        this.orderDetailId = orderDetailId;
        this.orderId = orderId;
        this.productId = productId;
        setQuantity(quantity);
        setUnitPrice(unitPrice);
    }

    public OrderDetail(String orderDetailId, String orderId, String productId,
            int quantity, double unitPrice) {
        this(orderDetailId, orderId, productId, quantity, BigDecimal.valueOf(unitPrice));
    }

    @Override
    public String getId() { return orderDetailId; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(orderDetailId), value(orderId), value(productId),
                Integer.toString(quantity), unitPrice.toPlainString());
    }

    public BigDecimal calculateSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getSubtotal() { return calculateSubtotal(); }

    private static String value(String value) { return value == null ? "" : value; }

    public String getOrderDetailId() { return orderDetailId; }
    @Override
    public void setId(String id) { this.orderDetailId = id; }
    public void setOrderDetailId(String orderDetailId) { this.orderDetailId = orderDetailId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive.");
        this.quantity = quantity;
    }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        Objects.requireNonNull(unitPrice, "Unit price cannot be null.");
        if (unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
        this.unitPrice = unitPrice;
    }
}

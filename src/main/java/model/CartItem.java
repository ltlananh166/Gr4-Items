package model;

import java.math.BigDecimal;
import java.util.Objects;

public class CartItem extends BaseEntity {
    private String cartItemId;
    private String cartId;
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;

    public CartItem() {
        this.quantity = 1;
        this.unitPrice = BigDecimal.ZERO;
    }

    public CartItem(String cartItemId, String cartId, String productId,
            int quantity, BigDecimal unitPrice) {
        setCartItemId(cartItemId);
        this.cartId = cartId;
        this.productId = productId;
        setQuantity(quantity);
        setUnitPrice(unitPrice);
    }

    public CartItem(String cartItemId, String cartId, String productId,
            int quantity, double unitPrice) {
        this(cartItemId, cartId, productId, quantity, BigDecimal.valueOf(unitPrice));
    }

    @Override
    public String getId() { return cartItemId; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(cartItemId), value(cartId), value(productId),
                Integer.toString(quantity), unitPrice.toPlainString());
    }

    public BigDecimal calculateSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private static String value(String value) { return value == null ? "" : value; }

    public String getCartItemId() { return cartItemId; }
    @Override
    public void setId(String id) { this.cartItemId = id; }
    public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }
    public String getCartId() { return cartId; }
    public void setCartId(String cartId) { this.cartId = cartId; }
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

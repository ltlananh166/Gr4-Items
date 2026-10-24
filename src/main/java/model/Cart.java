package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Cart extends BaseEntity {
    private String cartId;
    private String customerId;
    private final List<CartItem> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public Cart() { }

    public Cart(String cartId, String customerId) {
        this.cartId = cartId;
        this.customerId = customerId;
    }

    @Override
    public String getId() { return cartId; }

    @Override
    public String toCsvLine() {
        return String.join(",", value(cartId), value(customerId), calculateTotal().toPlainString());
    }

    public void addItem(CartItem item) {
        Objects.requireNonNull(item, "Cart item cannot be null.");
        if (item.getCartId() != null && !item.getCartId().equals(cartId)) {
            throw new IllegalArgumentException("Cart item belongs to a different cart.");
        }
        item.setCartId(cartId);
        items.add(item);
        calculateTotal();
    }

    public boolean removeItem(String cartItemId) {
        boolean removed = items.removeIf(item -> Objects.equals(item.getCartItemId(), cartItemId));
        if (removed) calculateTotal();
        return removed;
    }

    public void updateQuantity(String cartItemId, int quantity) {
        for (CartItem item : items) {
            if (Objects.equals(item.getCartItemId(), cartItemId)) {
                item.setQuantity(quantity);
                calculateTotal();
                return;
            }
        }
        throw new IllegalArgumentException("Cart item does not exist: " + cartItemId);
    }

    public BigDecimal calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) total = total.add(item.calculateSubtotal());
        totalAmount = total;
        return totalAmount;
    }

    private static String value(String value) { return value == null ? "" : value; }

    public String getCartId() { return cartId; }
    @Override
    public void setId(String id) { this.cartId = id; }
    public void setCartId(String cartId) { this.cartId = cartId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
    public List<CartItem> getCartItems() { return getItems(); }
    public BigDecimal getTotalAmount() { return calculateTotal(); }
}

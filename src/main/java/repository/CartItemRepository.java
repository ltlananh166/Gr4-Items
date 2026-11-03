package repository;

import model.CartItem;

import java.util.List;
import java.util.UUID;

public class CartItemRepository extends CsvRepository<CartItem> {
    public CartItemRepository() {
        this("data");
    }

    public CartItemRepository(String dataDirectory) {
        super(CartItem.class, dataDirectory, "cart_items.csv",
                "cartItemId,cartId,flashSaleItemId,productId,quantity,unitPrice");
    }

    public List<CartItem> findByCartId(String cartId) {
        return findAll().stream()
                .filter(item -> cartId != null && cartId.equals(item.getCartId()))
                .toList();
    }

    public String generateCartItemId() {
        return "CI-" + UUID.randomUUID();
    }

    public void deleteByCartItemId(String cartItemId) {
        if (cartItemId == null || cartItemId.isBlank()) {
            return;
        }
        List<CartItem> items = findAll();
        if (items.removeIf(item -> cartItemId.equals(item.getCartItemId()))) {
            saveAll(items);
        }
    }

    public void saveCart(List<CartItem> cartItems, String cartId) {
        List<CartItem> items = findAll();
        items.removeIf(item -> cartId.equals(item.getCartId()));
        items.addAll(cartItems);
        saveAll(items);
    }
}

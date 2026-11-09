package repository;

import model.CartItem;

import java.util.List;

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
        int maxId = findAll().stream()
                .map(CartItem::getCartItemId)
                .filter(id -> id != null && id.matches("CI-\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(3)))
                .max()
                .orElse(0);
        return "CI-" + (maxId + 1);
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

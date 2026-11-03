package controller;

import exception.InsufficientStockException;
import exception.InvalidOrderException;
import model.Cart;
import model.CartItem;
import service.CartService;
import service.FlashSaleService;

import java.util.List;

public class OrderController {
    private final FlashSaleService flashSaleService;
    private final CartService cartService;

    public OrderController(FlashSaleService flashSaleService) {
        this(flashSaleService, null);
    }

    public OrderController(FlashSaleService flashSaleService, CartService cartService) {
        this.flashSaleService = flashSaleService;
        this.cartService = cartService;
    }

    public boolean bookItem(String itemId, int quantity, String customerId)
            throws InvalidOrderException, InsufficientStockException {
        return flashSaleService.bookItem(itemId, quantity, customerId);
    }

    public boolean bookItem(String itemId, int quantity, String customerId, String voucherCode)
            throws InvalidOrderException, InsufficientStockException {
        return flashSaleService.bookItem(itemId, quantity, customerId, voucherCode);
    }

    public boolean approveOrder(String orderId) throws InvalidOrderException {
        return flashSaleService.approveOrder(orderId);
    }

    public boolean cancelOrder(String orderId) throws InvalidOrderException {
        return flashSaleService.cancelOrder(orderId);
    }

    public boolean completeOrder(String orderId) throws InvalidOrderException {
        return flashSaleService.completeOrder(orderId);
    }

    public void cancelAllPendingAndApprovedOrders(String customerId) {
        flashSaleService.cancelAllPendingAndApprovedOrders(customerId);
    }

    public Cart getCart(String customerId) {
        return requireCartService().getCart(customerId);
    }

    public CartItem addCartItem(String customerId, String itemId, int quantity) {
        return requireCartService().addItem(customerId, itemId, quantity);
    }

    public boolean removeCartItem(String customerId, String cartItemId) {
        return requireCartService().removeItem(customerId, cartItemId);
    }

    public CartService.ItemStatus getCartItemStatus(CartItem item) {
        return requireCartService().getItemStatus(item);
    }

    public List<CartService.CheckoutResult> checkoutCart(String customerId) {
        return requireCartService().checkout(customerId);
    }

    private CartService requireCartService() {
        if (cartService == null) {
            throw new IllegalStateException("Cart service is not configured.");
        }
        return cartService;
    }
}

// Thành viên 3

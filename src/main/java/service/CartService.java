package service;

import exception.InsufficientStockException;
import exception.InvalidOrderException;
import model.Cart;
import model.CartItem;
import model.FlashSaleEvent;
import model.FlashSaleItem;
import repository.CartItemRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Giỏ hàng được lưu trữ và kiểm tra khả năng mua theo trạng thái Flash Sale hiện tại. */
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final FlashSaleEventRepository eventRepository;
    private final FlashSaleService flashSaleService;

    public CartService(CartItemRepository cartItemRepository,
                       FlashSaleItemRepository flashSaleItemRepository,
                       FlashSaleEventRepository eventRepository,
                       FlashSaleService flashSaleService) {
        this.cartItemRepository = Objects.requireNonNull(cartItemRepository, "cartItemRepository");
        this.flashSaleItemRepository = Objects.requireNonNull(
                flashSaleItemRepository, "flashSaleItemRepository");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.flashSaleService = Objects.requireNonNull(flashSaleService, "flashSaleService");
    }

    public Cart getCart(String customerId) {
        String cartId = requireCustomerId(customerId);
        Cart cart = new Cart(cartId, cartId);
        cartItemRepository.findByCartId(cartId).forEach(cart::addItem);
        return cart;
    }

    public CartItem addItem(String customerId, String itemId, int quantity) {
        String cartId = requireCustomerId(customerId);
        if (quantity < 1 || quantity > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            throw new IllegalArgumentException("Each customer may add 1 to "
                    + FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT
                    + " items per product in an event.");
        }
        FlashSaleItem item = requireAvailableItem(itemId, quantity);
        Cart cart = getCart(cartId);
        CartItem existing = cart.getItems().stream()
                .filter(cartItem -> itemId.equals(cartItem.getFlashSaleItemId()))
                .findFirst()
                .orElse(null);
        int requestedQuantity = quantity + (existing == null ? 0 : existing.getQuantity());
        if (requestedQuantity > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            throw new IllegalArgumentException(
                    "Cart quantity exceeds the per-customer limit of "
                            + FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT + ".");
        }
        if (requestedQuantity > item.getRemainingStock()) {
            throw new IllegalArgumentException("Only " + item.getRemainingStock()
                    + " items are currently available.");
        }

        if (existing == null) {
            existing = new CartItem(cartItemRepository.generateCartItemId(), cartId,
                    item.getItemId(), item.getProductId(), quantity, item.getSalePrice());
        } else {
            existing.setQuantity(requestedQuantity);
            existing.setUnitPrice(item.getSalePrice());
        }
        CartItem updatedItem = existing;
        List<CartItem> updatedItems = new ArrayList<>(cart.getItems());
        if (cart.getItems().stream().noneMatch(
                cartItem -> updatedItem.getCartItemId().equals(cartItem.getCartItemId()))) {
            updatedItems.add(updatedItem);
        } else {
            updatedItems.replaceAll(cartItem ->
                    updatedItem.getCartItemId().equals(cartItem.getCartItemId())
                            ? updatedItem : cartItem);
        }
        cartItemRepository.saveCart(updatedItems, cartId);
        return updatedItem;
    }

    public boolean removeItem(String customerId, String cartItemId) {
        String cartId = requireCustomerId(customerId);
        CartItem item = cartItemRepository.findById(cartItemId);
        if (item == null || !cartId.equals(item.getCartId())) {
            return false;
        }
        cartItemRepository.deleteByCartItemId(cartItemId);
        return true;
    }

    public ItemStatus getItemStatus(CartItem cartItem) {
        FlashSaleItem item = cartItem == null ? null
                : flashSaleItemRepository.findById(cartItem.getFlashSaleItemId());
        if (item == null) {
            return new ItemStatus(null, "Unavailable: Flash Sale item no longer exists.");
        }
        FlashSaleEvent event = eventRepository.findById(item.getEventId());
        try {
            FlashSalePolicy.requireBookable(event, LocalDateTime.now());
        } catch (InvalidOrderException exception) {
            return new ItemStatus(item, exception.getMessage());
        }
        if (item.getRemainingStock() == 0) {
            return new ItemStatus(item, "Out of stock.");
        }
        if (item.getRemainingStock() < cartItem.getQuantity()) {
            return new ItemStatus(item, "Only " + item.getRemainingStock() + " items available.");
        }
        if (cartItem.getQuantity() > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            return new ItemStatus(item, "Quantity exceeds the per-customer limit.");
        }
        return new ItemStatus(item, "Available");
    }

    public List<CheckoutResult> checkout(String customerId) {
        String normalizedCustomerId = requireCustomerId(customerId);
        Cart cart = getCart(normalizedCustomerId);
        List<CheckoutResult> results = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            ItemStatus status = getItemStatus(cartItem);
            if (!status.available()) {
                results.add(new CheckoutResult(cartItem, false, status.message()));
                continue;
            }
            try {
                boolean placed = flashSaleService.bookItem(
                        cartItem.getFlashSaleItemId(), cartItem.getQuantity(), normalizedCustomerId);
                if (placed) {
                    cartItemRepository.deleteByCartItemId(cartItem.getCartItemId());
                    results.add(new CheckoutResult(cartItem, true, "Order placed successfully."));
                } else {
                    results.add(new CheckoutResult(
                            cartItem, false, "Unable to place the order. Please try again."));
                }
            } catch (InvalidOrderException | InsufficientStockException exception) {
                results.add(new CheckoutResult(cartItem, false, exception.getMessage()));
            }
        }
        return List.copyOf(results);
    }

    private FlashSaleItem requireAvailableItem(String itemId, int quantity) {
        FlashSaleItem item = flashSaleItemRepository.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Flash Sale item not found.");
        }
        ItemStatus status = getItemStatus(new CartItem(
                "validation", "validation", item.getItemId(), item.getProductId(),
                quantity, item.getSalePrice()));
        if (!status.available()) {
            throw new IllegalArgumentException(status.message());
        }
        return item;
    }

    private static String requireCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        return customerId.trim();
    }

    public record ItemStatus(FlashSaleItem item, String message) {
        public boolean available() {
            return "Available".equals(message);
        }

        public BigDecimal currentPrice() {
            return item == null ? BigDecimal.ZERO : item.getSalePrice();
        }
    }

    public record CheckoutResult(CartItem cartItem, boolean success, String message) {
    }
}

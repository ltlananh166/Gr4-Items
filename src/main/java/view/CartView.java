package view;

import controller.OrderController;
import exception.EndOfInputException;
import exception.OperationCancelledException;
import model.Cart;
import model.CartItem;
import service.CartService;
import service.FlashSaleConstants;

import java.math.BigDecimal;
import java.util.List;

/** Giao diện giỏ hàng của khách hàng. */
public class CartView {
    private final OrderController controller;
    private final ConsoleInput input;

    public CartView(OrderController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display(String customerId) {
        boolean open = true;
        while (open) {
            printCartMenu();
            try {
                int choice = input.readInt("Select an option", 0);
                switch (choice) {
                    case 1 -> viewItems(customerId);
                    case 2 -> addItem(customerId);
                    case 3 -> removeItem(customerId);
                    case 4 -> processToCheckout(customerId);
                    case 0 -> open = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (EndOfInputException | OperationCancelledException exception) {
                open = false;
            }
        }
    }

    public void printCartMenu() {
        System.out.println("\n--- CUSTOMER CART ---");
        System.out.println("1. View cart items");
        System.out.println("2. Add a Flash Sale item");
        System.out.println("3. Remove an item");
        System.out.println("4. Proceed to checkout");
        System.out.println("0. Return to the customer menu");
    }

    public void viewItems(String customerId) {
        Cart cart = controller.getCart(customerId);
        if (cart.getItems().isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        System.out.println("\n--- CART ITEMS ---");
        System.out.printf("%-4s | %-16s | %-12s | %-8s | %-14s | %s%n",
                "No.", "Cart item ID", "Sale item ID", "Quantity", "Current price", "Availability");
        System.out.println("-".repeat(104));
        List<CartItem> items = cart.getItems();
        for (int index = 0; index < items.size(); index++) {
            CartItem cartItem = items.get(index);
            CartService.ItemStatus status = controller.getCartItemStatus(cartItem);
            String productName = status.item() == null
                    ? cartItem.getProductId()
                    : status.item().getProductName();
            BigDecimal price = status.currentPrice();
            if (status.item() != null) {
                total = total.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
            System.out.printf("%-4d | %-16s | %-12s | %-8d | %,.0f VND | %s%n",
                    index + 1, cartItem.getCartItemId(), cartItem.getFlashSaleItemId(),
                    cartItem.getQuantity(), price, status.message());
            System.out.println("     Product: " + productName);
        }
        System.out.printf("Current listed total (not a checkout quote): %,.0f VND%n", total);
    }

    public void removeItem(String customerId) {
        Cart cart = controller.getCart(customerId);
        if (cart.getItems().isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }
        viewItems(customerId);
        String cartItemId = input.readStringRequired("Cart item ID to remove");
        if (controller.removeCartItem(customerId, cartItemId)) {
            System.out.println("Item removed from your cart.");
        } else {
            System.out.println("Cart item not found.");
        }
    }

    public void processToCheckout(String customerId) {
        Cart cart = controller.getCart(customerId);
        if (cart.getItems().isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }
        viewItems(customerId);
        String confirmation = input.readLine("Enter CHECKOUT to place available orders: ");
        if (!"CHECKOUT".equalsIgnoreCase(confirmation)) {
            System.out.println("Checkout cancelled. Your cart was not changed.");
            return;
        }
        for (CartService.CheckoutResult result : controller.checkoutCart(customerId)) {
            System.out.printf("%s: %s%n",
                    result.success() ? "SUCCESS" : "NOT PLACED",
                    result.message());
        }
    }

    public void addItem(String customerId) {
        try {
            String itemId = input.readStringRequired("Flash Sale item ID");
            int quantity = input.readIntMinMax("Quantity", 1,
                    FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT,
                    "Each customer may add 1 to "
                            + FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT
                            + " items per product.");
            controller.addCartItem(customerId, itemId, quantity);
            System.out.println("Item added to your cart. Inventory has not been reserved.");
        } catch (EndOfInputException | OperationCancelledException exception) {
            System.out.println("Add to cart cancelled.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.out.println("Unable to add item: " + exception.getMessage());
        }
    }
}
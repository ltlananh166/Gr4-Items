package view;

import model.Cart;
import model.CartItem;


public class CartView {


    public void displayCart(Cart cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            System.out.println("\n>> Your shopping cart is currently empty!");
            return;
        }

        System.out.println("\n----------------------------- YOUR SHOPPING CART -----------------------------");
        System.out.printf("%-12s | %-15s | %8s | %16s | %16s%n",
                "Item ID", "Product ID", "Quantity", "Unit Price (VND)", "Subtotal (VND)");
        System.out.println("-".repeat(78));

        for (CartItem item : cart.getItems()) {
            System.out.printf("%-12s | %-15s | %8d | %,16.0f | %,16.0f%n",
                    item.getCartItemId(),
                    item.getProductId(),
                    item.getQuantity(),
                    item.getUnitPrice().doubleValue(),
                    item.calculateSubtotal().doubleValue());
        }
        System.out.println("-".repeat(78));
        System.out.printf("TOTAL CART AMOUNT: %,.0f VND%n", cart.calculateTotal().doubleValue());
        System.out.println("------------------------------------------------------------------------------");
    }
}
package view;

public class OrderView {

    public void displayBookingResult(boolean success) {
        if (success) {
            System.out.println("Order successful.");
        } else {
            System.out.println("Order failed. Please check inventory, product code, or quantity limit.");
        }
    }

    public void displayBookingError(Exception e) {
        System.out.println("Order failed: " + e.getMessage());
    }
}
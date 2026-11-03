package view;

public class OrderView {
    public void displayBookingResult(boolean success) {
        if (success) {
            System.out.println("Order placed successfully.");
        } else {
            System.out.println("Order failed. Check stock, item ID, or quantity limits.");
        }
    }

    public void displayBookingError(Exception e) {
        System.out.println("Order failed: " + e.getMessage());
    }
}

// Thành viên 3

package view;

import controller.OrderTrackingController;

import java.util.Scanner;

/** Bộ định tuyến cho các màn hình lịch sử đơn hàng, chi tiết và giao dịch của khách hàng. */
public class OrderTrackingView {
    private final OrderHistoryView historyView;
    private final OrderDetailView detailView;
    private final ConsoleInput input;

    public OrderTrackingView(OrderTrackingController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public OrderTrackingView(OrderTrackingController controller, ConsoleInput input) {
        this.historyView = new OrderHistoryView(controller);
        this.detailView = new OrderDetailView(controller, input);
        this.input = input;
    }

    public void display(String customerId) {
        boolean back = false;
        while (!back) {
            printMenu();
            switch (input.readInt("Select an option", 0)) {
                case 1 -> historyView.displayOrders(customerId);
                case 2 -> detailView.display(customerId);
                case 3 -> historyView.displayTransactions(customerId);
                case 0 -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n===== MY ORDERS =====");
        System.out.println("1. List orders");
        System.out.println("2. View order details");
        System.out.println("3. Transaction history");
        System.out.println("0. Back");
    }
}

// Thành viên 3

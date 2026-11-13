package view;

import java.util.Scanner;

import controller.OrderTrackingController;

/** Bộ định tuyến cho các màn hình lịch sử đơn hàng, chi tiết và giao dịch của khách hàng. */
public class OrderTrackingView {
    private final OrderTrackingController controller;
    private final OrderHistoryView historyView;
    private final OrderDetailView detailView;
    private final ConsoleInput input;

    public OrderTrackingView(OrderTrackingController controller) {
        this(controller, new ConsoleInput(new Scanner(System.in)));
    }

    public OrderTrackingView(OrderTrackingController controller, ConsoleInput input) {
        this.controller = controller;
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
                case 4 -> cancelOrder(customerId);
                case 0 -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void printMenu() {
        System.out.println("\n===== MY ORDERS =====");
        System.out.println("1. List orders");
        System.out.println("2. View order details");
        System.out.println("3. Transaction history");
        System.out.println("4. Cancel my order");
        System.out.println("0. Back");
    }

    private void cancelOrder(String customerId) {
        String orderId = input.readStringRequired("Order ID to cancel");
        String confirmation = input.readLine(
                "Cancel order " + orderId + "? (Y/N): ");
        if (!"Y".equalsIgnoreCase(confirmation)) {
            System.out.println("Order cancellation cancelled.");
            return;
        }
        try {
            boolean cancelled = controller.cancelOrderByCustomer(orderId, customerId);
            System.out.println(cancelled ? "Order cancelled. Refund and stock restored."
                    : "Order was not cancelled.");
        } catch (exception.InvalidOrderException exception) {
            System.out.println("Unable to cancel order: " + exception.getMessage());
        }
    }
}

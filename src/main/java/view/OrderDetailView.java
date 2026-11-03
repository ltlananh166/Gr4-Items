package view;

import controller.OrderTrackingController;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;

import java.util.List;

/** Hóa đơn đơn hàng thuộc về khách hàng và lịch sử giao dịch đi kèm. */
public class OrderDetailView {
    private final OrderTrackingController controller;
    private final ConsoleInput input;

    public OrderDetailView(OrderTrackingController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display(String customerId) {
        String orderId = input.readStringRequired("Order ID");
        Order order = controller.getOrderById(orderId);
        if (order == null) {
            System.out.println("Order not found: " + orderId);
            return;
        }
        if (!customerId.equals(order.getCustomerId())) {
            System.out.println("You are not authorized to view this order.");
            return;
        }

        printHeader(order);
        printItems(controller.getOrderDetails(orderId));
        printTransactions(controller.getTransactionsByOrder(orderId));
    }

    private void printHeader(Order order) {
        System.out.println("\n--- ORDER DETAILS ---");
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Event: " + blankAsUnknown(order.getEventId()));
        System.out.println("Customer: " + order.getCustomerName());
        System.out.println("Order date: " + order.getOrderDate());
        System.out.println("Status: " + order.getStatus());
        System.out.printf("Total payment: %,.0f VND%n", order.getFinalAmount());
    }

    private void printItems(List<OrderDetail> details) {
        if (details.isEmpty()) {
            System.out.println("This order has no product details.");
            return;
        }
        System.out.printf("%n%-14s | %-30s | %4s | %14s | %15s%n",
                "Product ID", "Product name", "SL", "Unit price", "Subtotal");
        System.out.println("-".repeat(91));
        for (OrderDetail detail : details) {
            System.out.printf("%-14s | %-30s | %4d | %,14.0f | %,15.0f%n",
                    detail.getProductId(), trim(controller.getProductName(detail.getProductId()), 30),
                    detail.getQuantity(), detail.getUnitPrice(), detail.getSubtotal());
        }
    }

    private void printTransactions(List<OrderTransaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        System.out.println("\nProcessing history:");
        for (OrderTransaction transaction : transactions) {
            System.out.printf("- %s | %s | %s%n", transaction.getId(),
                    transaction.getStatus(), transaction.getMessage());
        }
    }

    private static String blankAsUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}

// Thành viên 3

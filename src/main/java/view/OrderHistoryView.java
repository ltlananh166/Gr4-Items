package view;

import controller.OrderTrackingController;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;

/** Các bảng đơn hàng và giao dịch của khách hàng. */
public class OrderHistoryView {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderTrackingController controller;

    public OrderHistoryView(OrderTrackingController controller) {
        this.controller = controller;
    }

    public void displayOrders(String customerId) {
        List<Order> orders = controller.getOrdersByCustomer(customerId);
        if (orders.isEmpty()) {
            System.out.println("You have no orders.");
            return;
        }

        System.out.println("\n--- ORDER HISTORY ---");
        System.out.printf("%-14s | %-30s | %-19s | %15s | %-12s%n",
                "Order ID", "Product", "Order date", "Total", "Status");
        System.out.println("-".repeat(105));
        for (Order order : orders) {
            System.out.printf("%-14s | %-30s | %-19s | %,15.0f | %-12s%n",
                    order.getOrderId(), productSummary(order.getOrderId()),
                    order.getOrderDate(), order.getTotalAmount(), order.getStatus());
        }
        long successCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.SUCCESS)
                .count();
        BigDecimal totalSpent = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.SUCCESS)
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.printf("Total: %d orders | Successful: %d | Spent: %,.0f VND%n",
                orders.size(), successCount, totalSpent);
    }

    public void displayTransactions(String customerId) {
        List<OrderTransaction> transactions = controller.getTransactionsByCustomer(customerId);
        if (transactions.isEmpty()) {
            System.out.println("You have no transactions.");
            return;
        }

        System.out.println("\n--- TRANSACTION HISTORY ---");
        System.out.printf("%-16s | %-14s | %-14s | %4s | %-10s | %-19s | %s%n",
                "Transaction ID", "Order ID", "Item ID", "SL", "Status", "Time", "Message");
        System.out.println("-".repeat(120));
        for (OrderTransaction transaction : transactions) {
            System.out.printf("%-16s | %-14s | %-14s | %4d | %-10s | %-19s | %s%n",
                    transaction.getId(), transaction.getOrderId(),
                    transaction.getItemId(), transaction.getQuantity(),
                    transaction.getStatus(), formatTimestamp(transaction.getTransactionTime()),
                    transaction.getMessage());
        }
    }

    private String productSummary(String orderId) {
        List<OrderDetail> details = controller.getOrderDetails(orderId);
        if (details.isEmpty()) {
            return "No details available";
        }
        String name = controller.getProductName(details.get(0).getProductId());
        if (details.size() > 1) {
            name += " +" + (details.size() - 1);
        }
        return trim(name, 30);
    }

    private static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? "N/A" : TIMESTAMP_FORMAT.format(timestamp);
    }

    private static String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}

// Thành viên 3

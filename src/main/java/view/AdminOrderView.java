package view;

import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;

import java.util.List;
import java.util.Objects;

public class AdminOrderView {
    private static final int DISPLAY_LIMIT = 30;

    private final OrderRepository orders;
    private final OrderDetailRepository details;
    private final OrderTransactionRepository transactions;
    private final ConsoleInput input;

    public AdminOrderView(OrderRepository orders, OrderDetailRepository details,
            OrderTransactionRepository transactions, ConsoleInput input) {
        this.orders = Objects.requireNonNull(orders, "orders");
        this.details = Objects.requireNonNull(details, "details");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.input = Objects.requireNonNull(input, "input");
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n===== ORDER DATA =====");
            System.out.println("1. List recent orders");
            System.out.println("2. View order details and transactions");
            System.out.println("0. Back");
            int choice = input.readIntMinMax("Choose an option", 0, 2,
                    "Choose 0, 1, or 2.");
            switch (choice) {
                case 1 -> listOrders();
                case 2 -> viewOrder();
                case 0 -> back = true;
                default -> throw new IllegalStateException("Validated order menu choice was out of range.");
            }
        }
    }

    private void listOrders() {
        List<Order> all = orders.findAll();
        System.out.printf("Orders: %d%n", all.size());
        System.out.printf("%-10s %-10s %-24s %-12s %-14s %-12s%n",
                "Order ID", "Customer", "Name", "Date", "Total", "Status");
        int start = Math.max(0, all.size() - DISPLAY_LIMIT);
        for (Order order : all.subList(start, all.size())) {
            System.out.printf("%-10s %-10s %-24s %-12s %,14.0f %-12s%n",
                    order.getOrderId(), order.getCustomerId(), trim(order.getCustomerName(), 24),
                    order.getOrderDate(), order.getTotalAmount().doubleValue(), order.getStatus());
        }
        if (all.size() > DISPLAY_LIMIT) {
            System.out.printf("Showing the latest %d orders.%n", DISPLAY_LIMIT);
        }
    }

    private void viewOrder() {
        String orderId = input.readStringRequired("Order ID: ");
        Order order = orders.findById(orderId);
        if (order == null) {
            System.out.println("Order not found.");
            return;
        }
        System.out.printf("%s | customer %s | %s | %,.0f VND | %s | event %s%n",
                order.getOrderId(), order.getCustomerId(), order.getCustomerName(),
                order.getTotalAmount().doubleValue(), order.getStatus(), order.getEventId());
        List<OrderDetail> orderDetails = details.findByOrderId(orderId);
        if (orderDetails.isEmpty()) {
            System.out.println("No order details.");
        } else {
            System.out.printf("%-12s %-12s %8s %14s %14s%n",
                    "Detail ID", "Product ID", "Quantity", "Unit price", "Subtotal");
            for (OrderDetail detail : orderDetails) {
                System.out.printf("%-12s %-12s %8d %,14.0f %,14.0f%n",
                        detail.getOrderDetailId(), detail.getProductId(), detail.getQuantity(),
                        detail.getUnitPrice().doubleValue(),
                        detail.calculateSubtotal().doubleValue());
            }
        }
        List<OrderTransaction> orderTransactions = transactions.findByOrderId(orderId);
        if (orderTransactions.isEmpty()) {
            System.out.println("No transaction records.");
        } else {
            for (OrderTransaction transaction : orderTransactions) {
                System.out.printf("Transaction %s | item %s | qty %d | %s | %s%n",
                        transaction.getId(), transaction.getItemId(), transaction.getQuantity(),
                        transaction.getStatus(), transaction.getMessage());
            }
        }
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 3) + "...";
    }
}

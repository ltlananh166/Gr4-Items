package view;

import controller.AdminOrderController;
import model.Order;
import model.OrderDetail;

import java.util.List;

/** Giao diện quản lý đơn hàng; các kiểm tra máy trạng thái vẫn nằm ở lớp dịch vụ. */
public class AdminOrderView {
    private static final int DISPLAY_LIMIT = 20;

    private final AdminOrderController controller;
    private final ConsoleInput input;

    public AdminOrderView(AdminOrderController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- ORDER MANAGEMENT ---");
            System.out.println("1. List orders");
            System.out.println("2. View and process an order");
            System.out.println("0. Back");
            try {
                switch (input.readInt("Select an option", 0)) {
                    case 1 -> displayOrders(controller.listOrders());
                    case 2 -> manageOrder();
                    case 0 -> back = true;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (exception.OperationCancelledException exception) {
                System.out.println("Order operation cancelled.");
            }
        }
    }

    public void manageOrder() {
        String orderId = input.readStringRequired("Order ID");
        Order order = controller.findOrder(orderId);
        if (order == null) {
            System.out.println("Order not found.");
            return;
        }
        displayOrderDetails(order, controller.getDetails(orderId));
        System.out.println("1. Approve");
        System.out.println("2. Cancel");
        System.out.println("3. Complete delivery");
        System.out.println("0. No change");
        int action = input.readInt("Select an action", 0);
        if (action == 0) {
            return;
        }
        try {
            boolean success = switch (action) {
                case 1 -> controller.approve(orderId);
                case 2 -> confirmCancel(orderId) && controller.cancel(orderId);
                case 3 -> controller.complete(orderId);
                default -> throw new IllegalArgumentException("Invalid action");
            };
            System.out.println(success ? "Status updated successfully." : "No changes made.");
        } catch (Exception exception) {
            System.out.println("Unable to update order: " + exception.getMessage());
        }
    }

    public boolean confirmCancel(String orderId) {
        return "CANCEL".equalsIgnoreCase(
                input.readLine("Enter CANCEL to confirm order cancellation " + orderId + ": "));
    }

    public void displayOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }
        System.out.printf("%-12s %-12s %-20s %15s %-12s %-12s%n",
                "Order ID", "Customer", "Date", "Total", "Status", "Event");
        System.out.println("-----------------------------------------------------------------------------------------");
        orders.stream().limit(DISPLAY_LIMIT).forEach(order ->
                System.out.printf("%-12s %-12s %-20s %,15.0f %-12s %-12s%n",
                        order.getOrderId(), order.getCustomerId(), order.getOrderDate(),
                        order.getTotalAmount(), order.getStatus(), order.getEventId()));
        if (orders.size() > DISPLAY_LIMIT) {
            System.out.println("... and " + (orders.size() - DISPLAY_LIMIT) + " orders.");
        }
    }

    public void displayOrderDetails(Order order, List<OrderDetail> details) {
        System.out.println("\nOrder " + order.getOrderId() + " - " + order.getStatus());
        System.out.println("Customer: " + order.getCustomerName());
        System.out.printf("Total payment: %,.0f VND%n", order.getTotalAmount());
        System.out.printf("%-12s %-30s %8s %12s %14s%n",
                "Product ID", "Name", "SL", "Unit price", "Subtotal");
        for (OrderDetail detail : details) {
            System.out.printf("%-12s %-30s %8d %,12.0f %,14.0f%n",
                    detail.getProductId(), trim(controller.getProductName(detail.getProductId()), 30),
                    detail.getQuantity(), detail.getUnitPrice(), detail.getSubtotal());
        }
    }

    public static String trim(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit - 3) + "...";
    }
}

// Thành viên 3

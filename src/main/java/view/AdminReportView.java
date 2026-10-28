package view;

import model.Order;
import model.enums.OrderStatus;
import repository.CustomerRepository;
import repository.FlashSaleEventRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.ProductRepository;
import repository.VoucherRepository;

import java.math.BigDecimal;
import java.util.Objects;

public class AdminReportView {
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final VoucherRepository vouchers;
    private final FlashSaleEventRepository events;
    private final FlashSaleItemRepository items;
    private final OrderRepository orders;
    private final OrderDetailRepository details;
    private final OrderTransactionRepository transactions;

    public AdminReportView(ProductRepository products, CustomerRepository customers,
            VoucherRepository vouchers, FlashSaleEventRepository events,
            FlashSaleItemRepository items, OrderRepository orders,
            OrderDetailRepository details, OrderTransactionRepository transactions) {
        this.products = Objects.requireNonNull(products, "products");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.vouchers = Objects.requireNonNull(vouchers, "vouchers");
        this.events = Objects.requireNonNull(events, "events");
        this.items = Objects.requireNonNull(items, "items");
        this.orders = Objects.requireNonNull(orders, "orders");
        this.details = Objects.requireNonNull(details, "details");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    public void display() {
        var allOrders = orders.findAll();
        BigDecimal revenue = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.SUCCESS
                        || order.getStatus() == OrderStatus.PAID)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("\n===== DATA & SALES SUMMARY =====");
        System.out.printf("Products: %d%n", products.findAll().size());
        System.out.printf("Customers: %d%n", customers.getAll().size());
        System.out.printf("Vouchers: %d%n", vouchers.getAll().size());
        System.out.printf("Flash Sale events: %d%n", events.findAll().size());
        System.out.printf("Flash Sale items: %d%n", items.findAll().size());
        System.out.printf("Orders: %d (success: %d, failed: %d)%n",
                allOrders.size(),
                allOrders.stream().filter(order -> order.getStatus() == OrderStatus.SUCCESS
                        || order.getStatus() == OrderStatus.PAID).count(),
                allOrders.stream().filter(order -> order.getStatus() == OrderStatus.FAILED).count());
        System.out.printf("Order detail rows: %d%n", details.findAll().size());
        System.out.printf("Transaction rows: %d%n", transactions.findAll().size());
        System.out.printf("Recorded revenue: %,.0f VND%n", revenue.doubleValue());
    }
}

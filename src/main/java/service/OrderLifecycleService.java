package service;

import exception.EntityNotFoundException;
import exception.InvalidOrderException;
import model.Customer;
import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.Voucher;
import model.enums.OrderStatus;
import model.enums.TransactionStatus;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;

import java.util.List;
import java.math.BigDecimal;

final class OrderLifecycleService {
    private static final String VOUCHER_MARKER = "Voucher: ";
    private static final String PAID_MARKER = "Paid: ";

    private final FlashSaleItemRepository flashItemRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final OrderTransactionRepository transactionRepository;

    OrderLifecycleService(FlashSaleItemRepository flashItemRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository,
                          CustomerRepository customerRepository,
                          VoucherRepository voucherRepository,
                          OrderTransactionRepository transactionRepository) {
        this.flashItemRepository = flashItemRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
        this.transactionRepository = transactionRepository;
    }

    public boolean approveOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            if (order.getStatus() != OrderStatus.PENDING) {
                throw new InvalidOrderException("Only PENDING orders can be approved.");
            }
            order.setStatus(OrderStatus.APPROVED);
            orderRepository.save(order);
            OrderContext context = resolveOrderContext(order);
            saveTransaction(order, context, OrderStatus.APPROVED,
                    "Order approved by admin.");
            return true;
        }
    }

    public boolean completeOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            if (order.getStatus() != OrderStatus.APPROVED) {
                throw new InvalidOrderException("Only APPROVED orders can be completed.");
            }
            order.setStatus(OrderStatus.SUCCESS);
            orderRepository.save(order);
            OrderContext context = resolveOrderContext(order);
            saveTransaction(order, context, OrderStatus.SUCCESS,
                    "Delivery successful | Order completed");
            return true;
        }
    }

    public boolean cancelOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            if (order.getStatus() == OrderStatus.SUCCESS || order.getStatus() == OrderStatus.PAID) {
                throw new InvalidOrderException("A paid order cannot be cancelled.");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new InvalidOrderException("The order has already been cancelled.");
            }
            return cancelOrder(order, "admin");
        }
    }

    public boolean cancelOrderByCustomer(String orderId, String customerId)
            throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            if (customerId == null || customerId.isBlank()
                    || !customerId.equals(order.getCustomerId())) {
                throw new InvalidOrderException("This order does not belong to your account.");
            }
            if (order.getStatus() != OrderStatus.PENDING
                    && order.getStatus() != OrderStatus.APPROVED) {
                throw new InvalidOrderException(
                        "Only PENDING or APPROVED orders can be cancelled by the customer.");
            }
            return cancelOrder(order, "customer");
        }
    }

    private boolean cancelOrder(Order order, String cancelledBy) {
        OrderContext context = resolveOrderContext(order);
        validateCancellationResources(order, context);

        restoreCustomerBalance(order);
        restoreInventory(order, context.details());
        restoreVoucher(order.getOrderId());

        order.cancelOrder();
        orderRepository.save(order);
        saveTransaction(order, context, OrderStatus.CANCELLED,
                "Order cancelled by " + cancelledBy
                        + ". Wallet funds, inventory, and voucher usage restored.");
        return true;
    }

    private void validateCancellationResources(Order order, OrderContext context) {
        if (customerRepository.findById(order.getCustomerId()).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot refund order: customer " + order.getCustomerId() + " was not found.");
        }
        for (OrderDetail detail : context.details()) {
            if (findItem(order, detail) == null) {
                throw new IllegalStateException("Cannot restore stock: Flash Sale item for product "
                        + detail.getProductId() + " was not found.");
            }
        }
        String voucherCode = extractVoucherCode(findInitialTransaction(order.getOrderId()));
        if (voucherCode != null
                && voucherRepository.findByCode(voucherCode).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot restore voucher usage: voucher " + voucherCode + " was not found.");
        }
    }

    public void cancelAllPendingAndApprovedOrders(String customerId) {
        synchronized (orderRepository) {
            List<Order> cancellableOrders = orderRepository.findAll().stream()
                    .filter(order -> customerId.equalsIgnoreCase(order.getCustomerId()))
                    .filter(order -> order.getStatus() == OrderStatus.PENDING
                            || order.getStatus() == OrderStatus.APPROVED)
                    .toList();
            for (Order order : cancellableOrders) {
                try {
                    cancelOrder(order.getOrderId());
                } catch (InvalidOrderException e) {
                    throw new IllegalStateException(
                            "Unable to cancel order " + order.getOrderId() + " for customer " + customerId, e);
                }
            }
        }
    }

    public Order requireOrder(String orderId) throws EntityNotFoundException {
        if (orderId == null || orderId.isBlank()) {
            throw new EntityNotFoundException("Order ID is required.");
        }
        Order order = orderRepository.findById(orderId.trim());
        if (order == null) {
            throw new EntityNotFoundException("Order not found.");
        }
        return order;
    }

    public void restoreCustomerBalance(Order order) {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
            if (customer == null) {
                throw new IllegalStateException(
                        "Cannot refund order: customer " + order.getCustomerId() + " was not found.");
            }
            customer.setWalletBalance(
                    customer.getWalletBalance() + getRefundAmount(order).doubleValue());
            customerRepository.save(customer);
        }
    }

    private BigDecimal getRefundAmount(Order order) {
        OrderTransaction initialTransaction = findInitialTransaction(order.getOrderId());
        BigDecimal paidAmount = extractPaidAmount(initialTransaction);
        if (paidAmount != null) {
            return paidAmount;
        }
        String voucherCode = extractVoucherCode(initialTransaction);
        if (voucherCode == null) {
            return order.getFinalAmount();
        }
        Voucher voucher = voucherRepository.findByCode(voucherCode).orElseThrow(() ->
                new IllegalStateException(
                        "Cannot determine refund amount: voucher " + voucherCode + " was not found."));
        return order.getTotalAmount().subtract(
                voucher.calculateDiscount(order.getTotalAmount())).max(BigDecimal.ZERO);
    }

    public void restoreInventory(Order order, List<OrderDetail> details) {
        synchronized (flashItemRepository) {
            for (OrderDetail detail : details) {
                FlashSaleItem item = findItem(order, detail);
                if (item != null) {
                    item.setSoldQty(Math.max(0, item.getSoldQty() - detail.getQuantity()));
                    item.setRemainingStock(item.getRemainingStock() + detail.getQuantity());
                    item.setVersion(item.getVersion() + 1);
                    flashItemRepository.save(item);
                }
            }
        }
    }

    public FlashSaleItem findItem(Order order, OrderDetail detail) {
        return flashItemRepository.findAll().stream()
                .filter(item -> detail.getProductId() != null
                        && detail.getProductId().equalsIgnoreCase(item.getProductId()))
                .filter(item -> order.getEventId() == null || order.getEventId().isBlank()
                        || order.getEventId().equalsIgnoreCase(item.getEventId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot restore stock: Flash Sale item for product "
                                + detail.getProductId() + " was not found."));
    }

    public void restoreVoucher(String orderId) {
        OrderTransaction initialTransaction = findInitialTransaction(orderId);
        String voucherCode = extractVoucherCode(initialTransaction);
        if (voucherCode == null) {
            return;
        }
        synchronized (voucherRepository) {
            Voucher voucher = voucherRepository.findByCode(voucherCode).orElse(null);
            if (voucher == null) {
                throw new IllegalStateException(
                        "Cannot restore voucher usage: voucher " + voucherCode + " was not found.");
            }
            if (voucher.getRemainingUses() != Integer.MAX_VALUE) {
                voucher.setRemainingUses(voucher.getRemainingUses() + 1);
                voucherRepository.save(voucher);
            }
        }
    }

    private OrderTransaction findInitialTransaction(String orderId) {
        return transactionRepository.findByOrderId(orderId).stream()
                .filter(transaction -> transaction.getMessage() != null
                        && transaction.getMessage().startsWith("Order placed"))
                .findFirst()
                .orElse(null);
    }

    public static BigDecimal extractPaidAmount(OrderTransaction transaction) {
        if (transaction == null || transaction.getMessage() == null) {
            return null;
        }
        int markerIndex = transaction.getMessage().indexOf(PAID_MARKER);
        if (markerIndex < 0) {
            return null;
        }
        String value = transaction.getMessage()
                .substring(markerIndex + PAID_MARKER.length()).trim();
        int delimiterIndex = value.indexOf('|');
        if (delimiterIndex >= 0) {
            value = value.substring(0, delimiterIndex).trim();
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            return amount.signum() < 0 ? null : amount;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Cannot determine refund amount from the order transaction.", exception);
        }
    }

    public static String extractVoucherCode(OrderTransaction transaction) {
        if (transaction == null || transaction.getMessage() == null) {
            return null;
        }
        int markerIndex = transaction.getMessage().indexOf(VOUCHER_MARKER);
        if (markerIndex < 0) {
            return null;
        }
        String voucherCode = transaction.getMessage()
                .substring(markerIndex + VOUCHER_MARKER.length()).trim();
        return voucherCode.equalsIgnoreCase("None") || voucherCode.isEmpty()
                ? null : voucherCode;
    }

    public OrderContext resolveOrderContext(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getOrderId());
        if (details.isEmpty()) {
            return new OrderContext("", 0, details);
        }
        OrderDetail firstDetail = details.get(0);
        FlashSaleItem firstItem = findItem(order, firstDetail);
        int totalQuantity = details.stream().mapToInt(OrderDetail::getQuantity).sum();
        return new OrderContext(firstItem != null ? firstItem.getItemId() : "",
                totalQuantity, details);
    }

    public void saveTransaction(Order order, OrderContext context,
                                 OrderStatus status, String message) {
        OrderTransaction transaction = new OrderTransaction(
                transactionRepository.generateNewTransactionId(), order.getOrderId(),
                order.getCustomerId(), java.math.BigDecimal.ZERO, "SYSTEM",
                toTransactionStatus(status), java.time.LocalDateTime.now());
        transaction.setItemId(context.itemId());
        transaction.setQuantity(context.quantity());
        transaction.setMessage(status.name() + " | " + message);
        transactionRepository.save(transaction);
    }

    public static TransactionStatus toTransactionStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> TransactionStatus.PENDING;
            case SUCCESS -> TransactionStatus.SUCCESS;
            default -> TransactionStatus.FAILED;
        };
    }

    private record OrderContext(String itemId, int quantity, List<OrderDetail> details) {
    }
}

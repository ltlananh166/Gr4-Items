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

final class OrderLifecycleService {
    private static final String VOUCHER_MARKER = "Voucher: ";

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

    boolean approveOrder(String orderId) throws InvalidOrderException {
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

    boolean completeOrder(String orderId) throws InvalidOrderException {
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

    boolean cancelOrder(String orderId) throws InvalidOrderException {
        synchronized (orderRepository) {
            Order order = requireOrder(orderId);
            if (order.getStatus() == OrderStatus.SUCCESS || order.getStatus() == OrderStatus.PAID) {
                throw new InvalidOrderException("A paid order cannot be cancelled.");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new InvalidOrderException("The order has already been cancelled.");
            }
            order.cancelOrder();
            OrderContext context = resolveOrderContext(order);

            restoreCustomerBalance(order);
            restoreInventory(order, context.details());
            restoreVoucher(order.getOrderId());

            orderRepository.save(order);
            saveTransaction(order, context, OrderStatus.CANCELLED,
                    "Order cancelled by admin. Wallet funds, inventory, and voucher usage restored.");
            return true;
        }
    }

    void cancelAllPendingAndApprovedOrders(String customerId) {
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

    private Order requireOrder(String orderId) throws EntityNotFoundException {
        if (orderId == null || orderId.isBlank()) {
            throw new EntityNotFoundException("Order ID is required.");
        }
        Order order = orderRepository.findById(orderId.trim());
        if (order == null) {
            throw new EntityNotFoundException("Order not found.");
        }
        return order;
    }

    private void restoreCustomerBalance(Order order) {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
            if (customer != null) {
                customer.setWalletBalance(
                        customer.getWalletBalance() + order.getTotalAmount().doubleValue());
                customerRepository.save(customer);
            }
        }
    }

    private void restoreInventory(Order order, List<OrderDetail> details) {
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

    private FlashSaleItem findItem(Order order, OrderDetail detail) {
        return flashItemRepository.findAll().stream()
                .filter(item -> detail.getProductId() != null
                        && detail.getProductId().equalsIgnoreCase(item.getProductId()))
                .filter(item -> order.getEventId() == null || order.getEventId().isBlank()
                        || order.getEventId().equalsIgnoreCase(item.getEventId()))
                .findFirst()
                .orElse(null);
    }

    private void restoreVoucher(String orderId) {
        OrderTransaction initialTransaction = transactionRepository.findAll().stream()
                .filter(transaction -> orderId.equalsIgnoreCase(transaction.getOrderId()))
                .filter(transaction -> transaction.getStatus() == TransactionStatus.PENDING)
                .findFirst()
                .orElse(null);
        String voucherCode = extractVoucherCode(initialTransaction);
        if (voucherCode == null) {
            return;
        }
        synchronized (voucherRepository) {
            Voucher voucher = voucherRepository.findByCode(voucherCode).orElse(null);
            if (voucher != null) {
                voucher.setRemainingUses(voucher.getRemainingUses() + 1);
                voucherRepository.save(voucher);
            }
        }
    }

    private static String extractVoucherCode(OrderTransaction transaction) {
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

    private OrderContext resolveOrderContext(Order order) {
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

    private void saveTransaction(Order order, OrderContext context,
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

    private static TransactionStatus toTransactionStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> TransactionStatus.PENDING;
            case SUCCESS -> TransactionStatus.SUCCESS;
            default -> TransactionStatus.FAILED;
        };
    }

    private record OrderContext(String itemId, int quantity, List<OrderDetail> details) {
    }
}

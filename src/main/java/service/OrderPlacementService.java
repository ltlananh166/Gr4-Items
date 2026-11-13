package service;

import exception.EntityNotFoundException;
import exception.InsufficientStockException;
import exception.InvalidOrderException;
import exception.PurchaseLimitExceededException;
import model.Customer;
import model.FlashSaleItem;
import model.FlashSaleEvent;
import model.Order;
import model.OrderDetail;
import model.OrderTransaction;
import model.Voucher;
import model.enums.CustTier;
import model.enums.OrderStatus;
import model.enums.TransactionStatus;
import repository.CustomerRepository;
import repository.FlashSaleItemRepository;
import repository.FlashSaleEventRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

final class OrderPlacementService {
    private final FlashSaleItemRepository flashItemRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final OrderTransactionRepository transactionRepository;
    private final FlashSaleEventRepository eventRepository;
    private final OrderRequestQueue requestQueue;

    OrderPlacementService(FlashSaleItemRepository flashItemRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository,
                          CustomerRepository customerRepository,
                          VoucherRepository voucherRepository,
                          OrderTransactionRepository transactionRepository,
                          FlashSaleEventRepository eventRepository,
                          OrderRequestQueue requestQueue) {
        this.flashItemRepository = flashItemRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.customerRepository = customerRepository;
        this.voucherRepository = voucherRepository;
        this.transactionRepository = transactionRepository;
        this.eventRepository = eventRepository;
        this.requestQueue = Objects.requireNonNull(requestQueue, "requestQueue");
    }

    public boolean placeOrder(String itemId, int quantity, String customerId, String voucherCode)
            throws InvalidOrderException, InsufficientStockException {
        String normalizedItemId = requireIdentifier(itemId, "Product ID (Item ID)");
        String normalizedCustomerId = requireIdentifier(customerId, "Customer ID");
        FlashSalePolicy.validateQuantity(quantity);

        Customer schedulingCustomer = requireCustomer(normalizedCustomerId);
        try (OrderRequestQueue.Permit ignored =
                     requestQueue.acquire(schedulingCustomer.getTier())) {
            synchronized (orderRepository) {
                Customer customer = requireCustomer(normalizedCustomerId);
                FlashSaleItem item = requireItem(normalizedItemId);
                FlashSaleEvent event = requireEvent(item.getEventId());
                FlashSalePolicy.requireBookable(event, LocalDateTime.now());
                enforceCumulativeLimit(customer.getCustomerId(), event.getEventId(),
                        item.getProductId(), quantity);

                BigDecimal unitPrice = calculateUnitPrice(item, customer.getTier());
                BigDecimal subtotal = multiplySafely(quantity, unitPrice);
                VoucherQuote voucherQuote = quoteVoucher(voucherCode, subtotal);
                BigDecimal totalAmount = subtotal.subtract(voucherQuote.discountAmount());
                Voucher reservedVoucher = reserveVoucher(voucherQuote.voucher());
                boolean debitCompleted = false;

                try {
                    debitWalletAndSellStock(customer.getCustomerId(), normalizedItemId,
                            quantity, totalAmount);
                    debitCompleted = true;
                    persistOrder(customer, event, item, quantity, unitPrice, subtotal,
                            totalAmount, voucherCode);
                    return true;
                } catch (RuntimeException exception) {
                    if (debitCompleted) {
                        rollbackPaymentAndStock(customer.getCustomerId(), normalizedItemId,
                                quantity, totalAmount);
                    }
                    restoreVoucher(reservedVoucher);
                    throw exception;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InvalidOrderException("The order request was interrupted.");
        }
    }

    public Customer requireCustomer(String customerId) throws InvalidOrderException {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            throw new EntityNotFoundException("Customer not found.");
        }
        if (customer.getStatus() == model.enums.CustomerStatus.BANNED) {
            throw new InvalidOrderException(
                    "Your account is banned. You cannot place orders.");
        }
        return customer;
    }

    public FlashSaleItem requireItem(String itemId) throws EntityNotFoundException {
        FlashSaleItem item = flashItemRepository.findById(itemId);
        if (item == null) {
            throw new EntityNotFoundException("Flash Sale item not found.");
        }
        return item;
    }

    public FlashSaleEvent requireEvent(String eventId) throws EntityNotFoundException {
        FlashSaleEvent event = eventRepository.findById(eventId);
        if (event == null) {
            throw new EntityNotFoundException("Flash Sale event not found.");
        }
        return event;
    }

    public void enforceCumulativeLimit(String customerId, String eventId,
                                        String productId, int requestedQuantity)
            throws PurchaseLimitExceededException {
        List<String> orderIds = orderRepository
                .findPurchasesCountingTowardLimit(customerId, eventId).stream()
                .map(Order::getOrderId)
                .toList();
        int alreadyBought = orderDetailRepository.totalQuantity(orderIds, productId);
        if (alreadyBought + requestedQuantity
                > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            throw new PurchaseLimitExceededException(String.format(
                    "You have purchased %d of this item in the event. The limit is %d items for all customer tiers.",
                    alreadyBought, FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT));
        }
    }

    public static BigDecimal calculateUnitPrice(FlashSaleItem item, CustTier tier) {
        return item.getSalePrice()
                .multiply(BigDecimal.ONE.subtract(
                        BigDecimal.valueOf(FlashSalePolicy.tierDiscountRate(tier))))
                .setScale(0, RoundingMode.HALF_UP);
    }

    public VoucherQuote quoteVoucher(String voucherCode, BigDecimal subtotal)
            throws InvalidOrderException {
        if (voucherCode == null || voucherCode.isBlank()) {
            return new VoucherQuote(null, BigDecimal.ZERO);
        }
        Voucher voucher = voucherRepository.findByCode(voucherCode.trim()).orElse(null);
        if (voucher == null) {
            throw new InvalidOrderException("Voucher code not found.");
        }
        if (!voucher.isValid()) {
            throw new InvalidOrderException("The voucher has no remaining uses.");
        }
        if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new InvalidOrderException(String.format(
                    "Order amount (%s) does not meet the voucher minimum (%s).",
                    subtotal.toPlainString(), voucher.getMinOrderValue().toPlainString()));
        }
        return new VoucherQuote(voucher, voucher.calculateDiscount(subtotal));
    }

    public Voucher reserveVoucher(Voucher voucher) throws InvalidOrderException {
        if (voucher == null) {
            return null;
        }
        synchronized (voucherRepository) {
            Voucher current = voucherRepository.findById(voucher.getVoucherId()).orElse(null);
            if (current == null || !current.consumeUse()) {
                throw new InvalidOrderException(
                        "The voucher ran out of uses due to concurrent requests.");
            }
            voucherRepository.save(current);
            return current;
        }
    }

    public void debitWalletAndSellStock(String customerId, String itemId,
                                         int quantity, BigDecimal totalAmount)
            throws InvalidOrderException, InsufficientStockException {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                throw new EntityNotFoundException("Customer not found.");
            }
            if (customer.getWalletBalance() < totalAmount.doubleValue()) {
                throw new InvalidOrderException(String.format(
                        "Insufficient wallet balance. Current balance: %,.0f VND. Amount due: %,.0f VND",
                        customer.getWalletBalance(), totalAmount));
            }
            if (!flashItemRepository.sellWithOptimisticLock(itemId, quantity)) {
                throw new InsufficientStockException(
                        "Insufficient stock or version conflict.");
            }
            double previousBalance = customer.getWalletBalance();
            try {
                customer.setWalletBalance(previousBalance - totalAmount.doubleValue());
                customerRepository.save(customer);
            } catch (RuntimeException e) {
                restoreStock(itemId, quantity);
                throw e;
            }
        }
    }

    public void rollbackPaymentAndStock(String customerId, String itemId,
                                         int quantity, BigDecimal totalAmount) {
        synchronized (customerRepository) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer != null) {
                customer.setWalletBalance(customer.getWalletBalance() + totalAmount.doubleValue());
                customerRepository.save(customer);
            }
        }
        restoreStock(itemId, quantity);
    }

    public void restoreStock(String itemId, int quantity) {
        synchronized (flashItemRepository) {
            FlashSaleItem item = flashItemRepository.findById(itemId);
            if (item != null) {
                item.setSoldQty(Math.max(0, item.getSoldQty() - quantity));
                item.setRemainingStock(item.getRemainingStock() + quantity);
                item.setVersion(item.getVersion() + 1);
                flashItemRepository.save(item);
            }
        }
    }

    public void persistOrder(Customer customer, FlashSaleEvent event, FlashSaleItem item,
                              int quantity, BigDecimal unitPrice, BigDecimal subtotal,
                              BigDecimal totalAmount,
                              String voucherCode) {
        String orderId = orderRepository.generateNewOrderId();
        Order order = new Order(orderId, customer.getCustomerId(), OrderStatus.PENDING);
        order.setCustomerName(customer.getName());
        order.setOrderDate(LocalDate.now().toString());
        order.setEventId(event.getEventId());
        order.setTotalAmount(subtotal);
        order.setDiscountAmount(subtotal.subtract(totalAmount));
        orderRepository.save(order);

        String detailId = orderDetailRepository.generateNewOrderDetailId();
        OrderDetail detail = new OrderDetail(detailId, orderId, item.getProductId(),
                quantity, unitPrice);
        orderDetailRepository.save(detail);

        String transactionId = transactionRepository.generateNewTransactionId();
        String voucherLabel = voucherCode == null || voucherCode.isBlank()
                ? "None" : voucherCode.trim();
        String message = String.format(
                "Order placed | Tier: %s | Quantity: %d | Voucher: %s | Paid: %s",
                customer.getTier() != null ? customer.getTier().name() : CustTier.STANDARD.name(),
                quantity, voucherLabel, totalAmount.toPlainString());
        OrderTransaction transaction = new OrderTransaction(
                transactionId, orderId, customer.getCustomerId(), totalAmount, "WALLET",
                TransactionStatus.PENDING, LocalDateTime.now());
        transaction.setItemId(item.getItemId());
        transaction.setQuantity(quantity);
        transaction.setMessage(message);
        transactionRepository.save(transaction);
    }

    public void restoreVoucher(Voucher voucher) {
        if (voucher == null) {
            return;
        }
        synchronized (voucherRepository) {
            Voucher current = voucherRepository.findById(voucher.getVoucherId()).orElse(null);
            if (current != null && current.getRemainingUses() != Integer.MAX_VALUE) {
                current.setRemainingUses(current.getRemainingUses() + 1);
                voucherRepository.save(current);
            }
        }
    }

    public static BigDecimal multiplySafely(int quantity, BigDecimal unitPrice)
            throws InvalidOrderException {
        try {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        } catch (ArithmeticException exception) {
            throw new InvalidOrderException("Order total exceeds the allowed limit.");
        }
    }

    public static String requireIdentifier(String value, String label)
            throws InvalidOrderException {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(label + " is required.");
        }
        return value.trim();
    }

    private record VoucherQuote(Voucher voucher, BigDecimal discountAmount) {
    }
}

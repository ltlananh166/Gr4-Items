package service;

import exception.InsufficientStockException;
import exception.InvalidOrderException;

public interface FlashSaleService {
    public boolean bookItem(String itemId, int quantity, String customerId) throws InvalidOrderException, InsufficientStockException;
    public boolean bookItem(String itemId, int quantity, String customerId, String voucherCode) throws InvalidOrderException, InsufficientStockException;
    public boolean approveOrder(String orderId) throws InvalidOrderException;
    public boolean cancelOrder(String orderId) throws InvalidOrderException;
    public boolean cancelOrderByCustomer(String orderId, String customerId)
            throws InvalidOrderException;
    public boolean completeOrder(String orderId) throws InvalidOrderException;
    public void cancelAllPendingAndApprovedOrders(String customerId);
}

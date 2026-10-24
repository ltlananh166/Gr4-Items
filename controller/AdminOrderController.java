package controller;

import model.Order;
import model.OrderDetails;
import service.AdminOrderService;
import java.util.List;

public class AdminCustomerController{
    private final AdminOrderService querService;
    private final OrderController OrderController;
    public AdminOrderController (AdminOrderService querService, OrderController ordController){
        this.queryService = queryService;
        this.orderController = orderController;
    }
    public List<Order> listOrder(){
        return queryService.listOrders();
    }
    public Order findOrder(String orderId){
        return queryService.findOrder(orderId);
    }
    public List<OrderDetail> getDetails(String orderId){
        return queryService.getDetails(orderId);
    }
    public String getProductName(String productId){
        return queryService.getProductName(productId);
    }
    public boolean approve(String orderId) throws Exception {
        return orderController.approvOrder(orderId);
    }
    public boolean cancel(String orderId) throws Exception{
        return orderController.cancelOrder(orderId);
    }
    public boolean complete(String orderId) throws Exception{
        return orderController.completeOrder(orderId);
    }
}

package service;
import model.Order;
import model.OrderDetails;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.ProductRepository;
import java.util.List;
public class AdminOrderService {
    private final OrderRepository OrderRepository;
    private final OrderDetailRepository detailRepository;
    private final ProductRepository ProductRepository;

    public AdminOrderService(OrderRepository orderRepository, OrderDetailRepository detailRepository,ProductRepository productRepository){
        this.orderRepository = orderRepository;
        this.detailRepository = detailRepository;
        this.productRepository = productRepository;
    }
    public List<Order>listOrders() {
        return orderRepository.findAll();
    }
    public Order findOrder(String orderId){
        return orderRepository.findById(orderId);
    }
    public List<OrderDetail>
    getDetails(String orderId ){
        return detailRepository.findByOrderId(orderId);
    }
    public String getProductName(String productId){
        var product = productRepository.findById(productId);
        return product ==null? "Khong ro san pham": product.getName();
    }
}
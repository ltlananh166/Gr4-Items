package service;

import model.Customer;
import model.Order;
import model.OrderTransaction;
import model.Voucher;
import model.enums.CustTier;
import model.enums.OrderStatus;
import model.repository.CustomerRepository;
import repository.OrderRepository;
import repository.OrderTransactionRepository;
import repository.VoucherRepository;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final OrderRepository orderRepository;
private final CustomerRepository customerRepository;
private final VoucherRepository voucherRepository;
private final OrderTransactionRepository transactionRepository;

public AdminReportService(OrderRepository orderRepository, CustomerRepository customerRepository,VoucherRepository voucherRepository, OrderTransactionRepository transactionRepository){
    this.orderRepository = Objects.requireNonNul(orderRepository,"orderRepository");
    this.customerRepository=Objects.requireNonNul(customerRepository,"customerRepository");
    this.voucherRepository=Objects.requireNonNul(voucherRepository,"voucherRepository");
    this.transactionRepository=Objects.requireNonNul(transactionRepository,"transactionRepository");
}
public Report generate(){
    List<Order> orders = orderRepository.finalAll();
    Map<OrderStatus,Long> orderCounts = countOrdersByStatus(orders);
    Set<String> successfulOrderIds = orders.stream().filter(order -> order.getOrderStatus() == OrderStatus.SUCCESS).map(Order:: getOrderId)
                                                                                                                   .collect(Collectors.toSet());
                                                                                                                long totalRevenue = orders.stream().filter(order -> order.getOrderStatus() == OrderStatus.SUCCESS)  
                                                                                                                   .mapToLong(Order::getTotalAmount).sum();

     Map<CustTier,Long> tierCounts = countCustomersByTier(customerRepository.findAll());
     Map<String,Long> successfullVoucherUsers = countSuccessfulVoucherUses(
        transactionRepository.findAll(),successfulOrderIds);
    List  <VoucherMetric> vouchers = voucherRepository.findAll().stream().sorted((first,second) -> first.getCode().compareToLgnoreCase(
        second.getCode())).map(voucher -> toMetric(voucher,succesfulVoucherUses)).toList();

    return new Report(totalRevebue,orderCounts.getOrdefault(OrderStatus.SUCCESS,0L),orders.size() - orderCounts.getOrDefault(OrderStatus.SUCCESS,0L),
    
    )                                                                                                            
}






package service;

import model.FlashSaleItem;
import model.Order;
import model.OrderDetail;
import model.RegularProduct;
import repository.FlashSaleItemRepository;
import repository.OrderDetailRepository;
import repository.OrderRepository;
import repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataNormalizationService {
    private final String dataDirectory;

    public DataNormalizationService(String dataDirectory) {
        if (dataDirectory == null || dataDirectory.isBlank()) {
            throw new IllegalArgumentException("Data directory is required.");
        }
        this.dataDirectory = dataDirectory;
    }

    public Map<String, Integer> normalizeRelations() {
        Map<String, RegularProduct> productsById = new LinkedHashMap<>();
        for (RegularProduct product : new ProductRepository(dataDirectory).findAll()) {
            productsById.put(product.getProductId(), product);
        }
        if (productsById.isEmpty()) {
            throw new IllegalStateException("Cannot normalize Flash Sale items without products.");
        }

        FlashSaleItemRepository items = new FlashSaleItemRepository(dataDirectory);
        List<FlashSaleItem> flashItems = items.findAll();
        Map<String, RegularProduct> productsForItems = new LinkedHashMap<>();
        for (FlashSaleItem item : flashItems) {
            RegularProduct product = productsById.get(item.getProductId());
            if (product == null) {
                throw new IllegalStateException("Flash Sale item " + item.getId()
                        + " references missing product " + item.getProductId() + ".");
            }
            if (item.getOriginalPrice().signum() <= 0
                    || product.getPrice().signum() <= 0) {
                throw new IllegalStateException("Cannot preserve the discount ratio for item "
                        + item.getId() + " with a zero price.");
            }
            productsForItems.put(item.getId(), product);
        }

        OrderRepository orders = new OrderRepository(dataDirectory);
        OrderDetailRepository details = new OrderDetailRepository(dataDirectory);
        Map<String, BigDecimal> totalsByOrderId = new LinkedHashMap<>();
        Map<String, Order> ordersById = new LinkedHashMap<>();
        for (Order order : orders.findAll()) {
            ordersById.put(order.getOrderId(), order);
        }
        for (OrderDetail detail : details.findAll()) {
            if (!ordersById.containsKey(detail.getOrderId())) {
                throw new IllegalStateException("Order detail " + detail.getOrderDetailId()
                        + " references missing order " + detail.getOrderId() + ".");
            }
            totalsByOrderId.merge(detail.getOrderId(), detail.calculateSubtotal(),
                    BigDecimal::add);
        }

        for (FlashSaleItem item : flashItems) {
            RegularProduct product = productsForItems.get(item.getId());
            BigDecimal saleRatio = item.getSalePrice()
                    .divide(item.getOriginalPrice(), 8, RoundingMode.HALF_UP);
            BigDecimal normalizedSalePrice = product.getPrice()
                    .multiply(saleRatio)
                    .setScale(0, RoundingMode.HALF_UP)
                    .max(BigDecimal.ONE)
                    .min(product.getPrice().subtract(BigDecimal.ONE));
            item.setProductName(product.getName());
            item.setOriginalPrice(product.getPrice());
            item.setSalePrice(normalizedSalePrice);
        }
        items.persist();

        List<Order> allOrders = new java.util.ArrayList<>(ordersById.values());
        for (Order order : allOrders) {
            BigDecimal total = totalsByOrderId.get(order.getOrderId());
            if (total != null) {
                order.setTotalAmount(total);
            }
        }
        orders.saveAll(allOrders);

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("flash_items.csv", flashItems.size());
        result.put("orders.csv", allOrders.size());
        return result;
    }
}

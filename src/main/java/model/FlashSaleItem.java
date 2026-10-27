package model;

import java.math.BigDecimal;
import java.util.Objects;

public class FlashSaleItem extends BaseEntity {
    private String itemId;
    private String eventId;
    private String productId;
    private String productName;
    private int initialStock;
    private int remainingStock;
    private BigDecimal originalPrice = BigDecimal.ZERO;
    private BigDecimal salePrice = BigDecimal.ZERO;
    private int soldQty;
    private int maxPerUser = 1;
    private int version;

    public FlashSaleItem() { }

    public FlashSaleItem(String id, String eventId, String productName, int stock, double price, int version) {
        this.itemId = id;
        this.eventId = eventId;
        this.productName = productName;
        this.initialStock = stock;
        setStock(stock);
        setSalePrice(BigDecimal.valueOf(price));
        setVersion(version);
    }

    public FlashSaleItem(String flashItemId, String eventId, String productId,
            BigDecimal originalPrice, BigDecimal salePrice, int quantity,
            int soldQuantity, int maxPerUser, int version) {
        this.itemId = flashItemId;
        this.eventId = eventId;
        this.productId = productId;
        this.originalPrice = nonNegative(originalPrice, "Original price");
        this.salePrice = nonNegative(salePrice, "Sale price");
        setSoldQuantity(soldQuantity);
        setStock(quantity - soldQuantity);
        this.initialStock = quantity;
        setMaxPerUser(maxPerUser);
        setVersion(version);
    }

    @Override
    public String getId() {
        return itemId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", value(itemId), value(productId), value(eventId), value(productName),
                originalPrice.toPlainString(), salePrice.toPlainString(), Integer.toString(initialStock),
                Integer.toString(soldQty), Integer.toString(remainingStock), Integer.toString(version));
    }

    public int getRemainingQuantity() {
        return remainingStock;
    }

    public boolean isAvailable() {
        return remainingStock > 0;
    }

    public boolean validateQuantity(int requestedQuantity, int alreadyPurchased) {
        return requestedQuantity > 0 && alreadyPurchased >= 0
                && requestedQuantity <= remainingStock
                && alreadyPurchased + requestedQuantity <= maxPerUser;
    }

    public boolean sell(int quantity) {
        if (!validateQuantity(quantity, 0)) return false;
        recordSale(quantity);
        return true;
    }

    public void recordSale(int quantity) {
        if (quantity < 1 || remainingStock < quantity) {
            throw new IllegalArgumentException("Sale quantity exceeds remaining stock.");
        }
        remainingStock -= quantity;
        soldQty += quantity;
        version++;
    }

    public void recordSaleFromObservedStock(int observedStock, int quantity) {
        if (quantity < 1 || observedStock < quantity) {
            throw new IllegalArgumentException("Sale quantity exceeds observed stock.");
        }
        remainingStock = observedStock - quantity;
        soldQty = initialStock - remainingStock;
        version++;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " cannot be null.");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " cannot be negative.");
        return value;
    }

    private static String value(String value) { return value == null ? "" : value; }

    public void setId(String id) {
        this.itemId = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getStock() {
        return remainingStock;
    }

    public void setStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        this.remainingStock = stock;
    }

    public double getPrice() {
        return salePrice.doubleValue();
    }

    public void setPrice(double price) {
        setSalePrice(BigDecimal.valueOf(price));
    }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = nonNegative(originalPrice, "Original price");
    }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = nonNegative(salePrice, "Sale price");
    }
    public int getQuantity() { return initialStock; }
    public void setQuantity(int quantity) {
        if (quantity < soldQty) throw new IllegalArgumentException("Quantity cannot be below sold quantity.");
        initialStock = quantity;
        remainingStock = quantity - soldQty;
    }
    public int getInitialStock() { return initialStock; }
    public void setInitialStock(int initialStock) {
        if (initialStock < 0) throw new IllegalArgumentException("Initial stock cannot be negative.");
        this.initialStock = initialStock;
    }
    public int getSoldQuantity() { return soldQty; }
    public void setSoldQuantity(int soldQuantity) {
        if (soldQuantity < 0) throw new IllegalArgumentException("Sold quantity cannot be negative.");
        this.soldQty = soldQuantity;
    }
    public int getSoldQty() { return soldQty; }
    public void setSoldQty(int soldQty) { setSoldQuantity(soldQty); }
    public int getRemainingStock() { return remainingStock; }
    public void setRemainingStock(int remainingStock) { setStock(remainingStock); }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void resetToInitialStock() {
        remainingStock = initialStock;
        soldQty = 0;
        version++;
    }
    public int getMaxPerUser() { return maxPerUser; }
    public void setMaxPerUser(int maxPerUser) {
        if (maxPerUser < 1) throw new IllegalArgumentException("Per-user limit must be positive.");
        this.maxPerUser = maxPerUser;
    }
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version < 0) throw new IllegalArgumentException("Version cannot be negative.");
        this.version = version;
    }
}

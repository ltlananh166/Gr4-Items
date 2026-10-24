package model;

import java.math.BigDecimal;
import java.util.Objects;

public class FlashSaleItem extends BaseEntity {
    private String id;
    private String eventId;
    private String productId;
    private String productName;
    private int stock;
    private BigDecimal originalPrice = BigDecimal.ZERO;
    private BigDecimal salePrice = BigDecimal.ZERO;
    private int soldQuantity;
    private int maxPerUser = 1;
    private int version;

    public FlashSaleItem() { }

    public FlashSaleItem(String id, String eventId, String productName, int stock, double price, int version) {
        this.id = id;
        this.eventId = eventId;
        this.productName = productName;
        setStock(stock);
        setSalePrice(BigDecimal.valueOf(price));
        setVersion(version);
    }

    public FlashSaleItem(String flashItemId, String eventId, String productId,
            BigDecimal originalPrice, BigDecimal salePrice, int quantity,
            int soldQuantity, int maxPerUser, int version) {
        this.id = flashItemId;
        this.eventId = eventId;
        this.productId = productId;
        this.originalPrice = nonNegative(originalPrice, "Original price");
        this.salePrice = nonNegative(salePrice, "Sale price");
        setSoldQuantity(soldQuantity);
        setStock(quantity - soldQuantity);
        setMaxPerUser(maxPerUser);
        setVersion(version);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", value(id), value(eventId), value(productId), value(productName),
                originalPrice.toPlainString(), salePrice.toPlainString(), Integer.toString(getQuantity()),
                Integer.toString(soldQuantity), Integer.toString(maxPerUser), Integer.toString(version));
    }

    public int getRemainingQuantity() {
        return stock;
    }

    public boolean isAvailable() {
        return stock > 0;
    }

    public boolean validateQuantity(int requestedQuantity, int alreadyPurchased) {
        return requestedQuantity > 0 && alreadyPurchased >= 0
                && requestedQuantity <= stock
                && alreadyPurchased + requestedQuantity <= maxPerUser;
    }

    public boolean sell(int quantity) {
        if (!validateQuantity(quantity, 0)) return false;
        stock -= quantity;
        soldQuantity += quantity;
        version++;
        return true;
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " cannot be null.");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " cannot be negative.");
        return value;
    }

    private static String value(String value) { return value == null ? "" : value; }

    public void setId(String id) {
        this.id = id;
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
        return stock;
    }

    public void setStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        this.stock = stock;
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
    public int getQuantity() { return stock + soldQuantity; }
    public void setQuantity(int quantity) {
        if (quantity < soldQuantity) throw new IllegalArgumentException("Quantity cannot be below sold quantity.");
        stock = quantity - soldQuantity;
    }
    public int getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(int soldQuantity) {
        if (soldQuantity < 0) throw new IllegalArgumentException("Sold quantity cannot be negative.");
        this.soldQuantity = soldQuantity;
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

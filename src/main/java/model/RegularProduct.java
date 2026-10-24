package model;

import java.math.BigDecimal;
import java.util.Objects;

public class RegularProduct extends BaseEntity {
    private String productId;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private int stock;
    private int version;
    private String status;

    public RegularProduct() {
        this.price = BigDecimal.ZERO;
        this.status = "ACTIVE";
    }

    public RegularProduct(String productId, String name, String description, String category,
            BigDecimal price, int stock, int version, String status) {
        setProductId(productId);
        this.name = name;
        this.description = description;
        this.category = category;
        setPrice(price);
        setStock(stock);
        setVersion(version);
        setStatus(status);
    }

    public RegularProduct(String productId, String name, String description, String category,
            double price, int stock, int version, String status) {
        this(productId, name, description, category, BigDecimal.valueOf(price), stock, version, status);
    }

    @Override
    public String getId() {
        return productId;
    }

    @Override
    public String toCsvLine() {
        return String.join(",", nullToEmpty(productId), nullToEmpty(name), nullToEmpty(description),
                nullToEmpty(category), price.toPlainString(), Integer.toString(stock),
                Integer.toString(version), nullToEmpty(status));
    }

    public boolean isAvailable() {
        return stock > 0 && "ACTIVE".equalsIgnoreCase(status);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public String getProductId() { return productId; }
    @Override
    public void setId(String id) { this.productId = id; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) {
        Objects.requireNonNull(price, "Price cannot be null.");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        this.price = price;
    }
    public void setPrice(double price) { setPrice(BigDecimal.valueOf(price)); }
    public int getStock() { return stock; }
    public void setStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative.");
        this.stock = stock;
    }
    public int getVersion() { return version; }
    public void setVersion(int version) {
        if (version < 0) throw new IllegalArgumentException("Version cannot be negative.");
        this.version = version;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status == null || status.trim().isEmpty() ? "ACTIVE" : status.trim().toUpperCase();
    }
}

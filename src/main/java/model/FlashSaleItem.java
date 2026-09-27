package model;

public class FlashSaleItem {
    private String id;
    private String eventId;
    private String productName;
    private int stock;
    private double price;
    private int version;

    public FlashSaleItem(){
        
    }

    public FlashSaleItem(String id, String eventId, String productName, int stock, double price, int version) {
        this.id = id;
        this.eventId = eventId;
        this.productName = productName;
        this.stock = stock;
        this.price = price;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

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
        this.stock = stock;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}

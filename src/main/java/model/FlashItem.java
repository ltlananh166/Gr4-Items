/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 * Flash Sale Item - san pham tham gia flash sale.
 * Chua truong version cho Optimistic Locking.
 */
public class FlashItem extends BaseEntity{
    private String itemId;
    private String productId;
    private String eventId;
    private String productName;
    private int originalPrice;
    private int salePrice;
    private int inititalStock;
    private int soldQty;
    private int remainingStock;
    private int version;
    
    public FlashItem() {}

    public FlashItem(String itemId, String productId, String eventId, String productName, int originalPrice, int salePrice, int inititalStock, int soldQty, int remainingStock, int version) {
        this.itemId = itemId;
        this.productId = productId;
        this.eventId = eventId;
        this.productName = productName;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.inititalStock = inititalStock;
        this.soldQty = 0;
        this.remainingStock = remainingStock;
        this.version = 0;
    }
    
    public FlashItem(String itemId, String productId, String eventId, String productName, int originalPrice, int salePrice, int initialStock, int soldQty, int version) {
        this.itemId= itemId;
        this.productId = productId;
        this.eventId = eventId;
        this.productName = productName;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.inititalStock = inititalStock;
        this.soldQty = soldQty;
        this.remainingStock = initialStock - soldQty;
        this.version = version; 
    }
    
    
    @Override
    public String getId(){
        return itemId;
    }
    
    @Override
    public String toCsvLine(){
        return  itemId + "," + productId + "," + eventId + "," + productName + ","
                + originalPrice + "," + salePrice + ","
                + inititalStock + "," + soldQty + "," + version;
    }
    
    public static FlashItem fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split(",");
        if (parts.length < 9) return null;
        
        FlashItem item = new FlashItem();
        item.setItemId(parts[0].trim());
        item.setProductId(parts[1].trim());
        item.setEventId(parts[2].trim());
        item.setProductName(parts[3].trim());
        item.setOriginalPrice(Integer.parseInt(parts[4].trim()));
        item.setSalePrice(Integer.parseInt(parts[6].trim()));
        item.setSoldQty(Integer.parseInt(parts[7].trim()));
        
        if(parts.length >= 10) {
            item.setRemainingStock(Integer.parseInt(parts[8].trim()));
            item.setVersion(Integer.parseInt(parts[9].trim()));
        } 
        else {
            item.setRemainingStock(item.getInititalStock() - item.getSoldQty());
            item.setVersion(Integer.parseInt(parts[8].trim()));
            
        }
        return item;
    }
    

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    public int getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(int originalPrice) {
        this.originalPrice = originalPrice;
    }

    public int getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(int salePrice) {
        this.salePrice = salePrice;
    }

    public int getInititalStock() {
        return inititalStock;
    }

    public void setInititalStock(int inititalStock) {
        this.inititalStock = inititalStock;
    }

    public int getSoldQty() {
        return soldQty;
    }

    public void setSoldQty(int soldQty) {
        this.soldQty = soldQty;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(int remainingStock) {
        this.remainingStock = remainingStock;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    
    
    
}

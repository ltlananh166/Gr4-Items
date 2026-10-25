/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;


public class OrderDetail extends BaseEntity {

    private String detailId;
    private String orderId;
    private String productId;
    private int quantity;
    private int unitPrice;
    private int subtotal;
    
    public OrderDetail(){}

    public OrderDetail(String detailId, String orderId, String productId, int quantity, int unitPrice, int subtotal) {
        this.detailId = detailId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }
    
    @Override
    public String getId() {
        return detailId;
    }
    
    @Override
    public String toCsvLine(){
        return detailId + "," + orderId + "," + productId + "," + quantity + "," + unitPrice + "," + subtotal;
    }
    
    public static OrderDetail fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split(",", -1);
        if(parts.length < 6) return null;
        return new OrderDetail(
            parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                Integer.parseInt(parts[3].trim()),
                Integer.parseInt(parts[4].trim()),
                Integer.parseInt(parts[5].trim())
        );
    }

    public String getDetailId() {
        return detailId;
    }

    public void setDetailId(String detailId) {
        this.detailId = detailId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(int subtotal) {
        this.subtotal = subtotal;
    }
    
    
}

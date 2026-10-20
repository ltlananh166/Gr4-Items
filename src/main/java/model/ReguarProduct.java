/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;
import java.math.BigDecimal;
/**
 *
 * @author LAPTOP
 */
public class ReguarProduct extends BaseEntity {
    private String productId;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private int stock;
    private int version;
    private String status;

    public ReguarProduct(String productId, String name, String description, 
            String category, BigDecimal price, int stock, int version, String status) {
        super(productId);
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.version = version;
        this.status = status;
    }
   public boolean isAvailable() {
        return stock > 0 && "ACTIVE".equalsIgnoreCase(status);
        
    }
    public BigDecimal getPrice(){
        return price;
    }
    public String getProductId(){
        return productId;
    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
        updateTimestamp();
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
        updateTimestamp();
    }
    public String getCategry(){
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        updateTimestamp();
    }
    public void setPrice(BigDecimal price){
        if (price == null || price.signum()<0){
            throw new IllegalArgumentException("Invalid price");
        }
        this.price = price;
        updateTimestamp();
    }
    public void setStock(int stock){
        if (stock <0){
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.stock = stock;
        updateTimestamp();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
        updateTimestamp();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        updateTimestamp();
    }
    
}

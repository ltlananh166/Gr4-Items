/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.math.BigDecimal;

@Deprecated
public class ReguarProduct extends RegularProduct {
    public ReguarProduct(String productId, String name, String description,
            String category, BigDecimal price, int stock, int version, String status) {
        super(productId, name, description, category, price, stock, version, status);
    }

    public String getCategry() {
        return getCategory();
    }
}

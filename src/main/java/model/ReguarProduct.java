package model;

import java.math.BigDecimal;

/**
 * Compatibility alias for the old misspelled class name.
 * New code should use {@link RegularProduct}.
 */
@Deprecated
public class ReguarProduct extends RegularProduct {

    public ReguarProduct(String productId, String name, String description,
                         String category, BigDecimal price, int stock,
                         int version, String status) {
        super(productId, name, description, category, price, stock, version, status);
    }
}

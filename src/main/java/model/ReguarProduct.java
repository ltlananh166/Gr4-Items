package model;

import java.math.BigDecimal;

/**
 * Bí danh tương thích với tên lớp cũ bị viết sai chính tả.
 * Mã mới nên sử dụng {@link RegularProduct}.
 */
@Deprecated
public class ReguarProduct extends RegularProduct {

    public ReguarProduct(String productId, String name, String description,
                         String category, BigDecimal price, int stock,
                         int version, String status) {
        super(productId, name, description, category, price, stock, version, status);
    }
}

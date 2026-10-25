package repository;

import model.Product;

public class ProductRepository extends CsvRepository<Product> {
    public ProductRepository() {
        super(Product.class, "products.csv", "productId,name,brand,category,price,stock,description,version");
    }

    public ProductRepository(String dataDirectory) {
        super(Product.class, dataDirectory, "products.csv", "productId,name,brand,category,price,stock,description,version");
    }

    public String generateNewProductId() {
        int maxId = findAll().stream()
                .map(Product::getProductId)
                .filter(id -> id != null && id.startsWith("P-"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return String.format("P-%05d", maxId + 1);
    }
}
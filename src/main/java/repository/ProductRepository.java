
package repository;

import model.RegularProduct;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ProductRepository extends CsvRepository<RegularProduct> {

    private static final String FILE_NAME = "products.csv";

    private static final String HEADER =
            "productId,name,brand,category,price,stock,description,version";

    public ProductRepository() {
        super(RegularProduct.class, FILE_NAME, HEADER);
    }

    public ProductRepository(String dataDirectory) {
        super(RegularProduct.class, dataDirectory, FILE_NAME, HEADER);
    }

    @Override
    public boolean update(RegularProduct product, int expectedVersion) {
        if (product == null) {
            throw new IllegalArgumentException("Updated product must not be null.");
        }

        int originalVersion = product.getVersion();
        product.setVersion(Math.addExact(expectedVersion, 1));
        boolean updated = false;
        try {
            updated = super.update(product, expectedVersion);
            return updated;
        } finally {
            if (!updated) {
                product.setVersion(originalVersion);
            }
        }
    }

    public String generateNewProductId() {
        int maxId = findAll().stream()
                .map(RegularProduct::getProductId)
                .filter(id -> id != null && id.matches("P-\\d{5}"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);

        return String.format("P-%05d", maxId + 1);
    }

    public List<RegularProduct> findByCategory(String category) {
        return findAll().stream()
                .filter(product -> category != null
                        && category.equalsIgnoreCase(product.getCategory()))
                .collect(Collectors.toList());
    }

    public List<RegularProduct> findAvailableProducts() {
        return findAll().stream()
                .filter(RegularProduct::isAvailable)
                .collect(Collectors.toList());
    }

    public List<RegularProduct> findByStatus(String status) {
        return findAll().stream()
                .filter(product -> status != null
                        && status.equalsIgnoreCase(product.getStatus()))
                .collect(Collectors.toList());
    }

    public boolean deleteById(String productId) {
        List<RegularProduct> products = findAll();
        boolean removed = products.removeIf(
                product -> productId != null && productId.equals(product.getProductId()));
        if (removed) {
            saveAll(products);
        }
        return removed;
    }

    public List<RegularProduct> searchByCategoryAndPriceRange(
            String category, int minimumPrice, int maximumPrice) {
        if (minimumPrice < 0 || maximumPrice < minimumPrice) {
            throw new IllegalArgumentException("Invalid product price range.");
        }
        BigDecimal minimum = BigDecimal.valueOf(minimumPrice);
        BigDecimal maximum = BigDecimal.valueOf(maximumPrice);
        return findAll().stream()
                .filter(product -> category == null || category.isBlank()
                        || category.equalsIgnoreCase(product.getCategory()))
                .filter(product -> product.getPrice().compareTo(minimum) >= 0
                        && product.getPrice().compareTo(maximum) <= 0)
                .collect(Collectors.toList());
    }
}

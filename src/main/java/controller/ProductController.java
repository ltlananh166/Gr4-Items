package controller;

import model.RegularProduct;
import repository.ProductRepository;

import java.util.List;

public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public RegularProduct createProduct(RegularProduct product) {
        if (product == null || product.getProductId() == null
                || !product.getProductId().matches("P-\\d+")) {
            throw new IllegalArgumentException(
                    "Product ID must be P- followed by digits (for example, P-5001).");
        }
        if (productRepository.findById(product.getProductId()) != null) {
            throw new IllegalArgumentException(
                    "Product ID already exists: " + product.getProductId());
        }
        productRepository.save(product);
        return product;
    }

    public RegularProduct findProduct(String productId) {
        return productRepository.findById(productId);
    }

    public List<RegularProduct> listProducts() {
        return productRepository.findAll();
    }

    public boolean updateProduct(RegularProduct product, int expectedVersion) {
        return productRepository.update(product, expectedVersion);
    }

    public boolean deleteProduct(String productId) {
        return productRepository.deleteById(productId);
    }

    public List<RegularProduct> searchProducts(String category, int minimumPrice, int maximumPrice) {
        return productRepository.searchByCategoryAndPriceRange(
                category, minimumPrice, maximumPrice);
    }
}

// Thành viên 3

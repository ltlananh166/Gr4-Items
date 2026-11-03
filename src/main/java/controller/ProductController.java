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

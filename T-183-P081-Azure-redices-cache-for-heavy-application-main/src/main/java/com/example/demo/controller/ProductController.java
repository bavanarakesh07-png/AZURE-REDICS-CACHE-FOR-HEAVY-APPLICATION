package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ==========================================
    // GET ALL PRODUCTS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {

        List<Product> products =
                productService.getAllProducts();

        return ResponseEntity.ok(products);
    }


    // ==========================================
    // GET PRODUCT BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @PathVariable Long id) {

        Product product =
                productService.getProduct(id);

        if (product == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(product);
    }


    // ==========================================
    // CREATE PRODUCT
    // ==========================================

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestBody Product product) {

        Product savedProduct =
                productService.createProduct(product);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedProduct);
    }


    // ==========================================
    // UPDATE PRODUCT
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {

        Product updatedProduct =
                productService.updateProduct(
                        id,
                        product
                );

        if (updatedProduct == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(updatedProduct);
    }


    // ==========================================
    // DELETE PRODUCT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id) {

        boolean deleted =
                productService.deleteProduct(id);

        if (!deleted) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                "Product deleted successfully"
        );
    }
}
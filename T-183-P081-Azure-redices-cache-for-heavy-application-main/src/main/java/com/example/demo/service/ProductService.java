package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import redis.clients.jedis.UnifiedJedis;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UnifiedJedis redis;
    private final ObjectMapper objectMapper;
    private final CacheStatsService cacheStatsService;

    private static final String CACHE_PREFIX = "product:";
    private static final String ALL_PRODUCTS_KEY = "products:all";
    private static final int CACHE_TTL = 300;

    public ProductService(
            ProductRepository productRepository,
            UnifiedJedis redis,
            ObjectMapper objectMapper,
            CacheStatsService cacheStatsService) {

        this.productRepository = productRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.cacheStatsService = cacheStatsService;
    }

    // ==========================================
    // GET PRODUCT BY ID
    // ==========================================

    public Product getProduct(Long id) {

        String key = CACHE_PREFIX + id;

        // Start timing Redis lookup
        long redisStart = System.nanoTime();

        try {

            String cachedProduct = redis.get(key);

            long redisTime =
                    (System.nanoTime() - redisStart) / 1_000_000;

            if (cachedProduct != null) {

                // Record cache hit
                cacheStatsService.recordHit();

                System.out.println(
                        "CACHE HIT -> Product " + id
                );

                System.out.println(
                        "REDIS RESPONSE TIME -> "
                                + redisTime + " ms"
                );

                return objectMapper.readValue(
                        cachedProduct,
                        Product.class
                );
            }

            // Record cache miss
            cacheStatsService.recordMiss();

            System.out.println(
                    "CACHE MISS -> Product " + id
            );

            System.out.println(
                    "REDIS LOOKUP TIME -> "
                            + redisTime + " ms"
            );

        } catch (Exception e) {

            // Redis failed, treat this request as a cache miss
            cacheStatsService.recordMiss();

            long redisTime =
                    (System.nanoTime() - redisStart) / 1_000_000;

            System.out.println(
                    "REDIS ERROR AFTER "
                            + redisTime
                            + " ms"
            );

            System.out.println(
                    "Redis unavailable. Reading from MySQL."
            );
        }

        // Start timing MySQL
        long mysqlStart = System.nanoTime();

        Optional<Product> product =
                productRepository.findById(id);

        long mysqlTime =
                (System.nanoTime() - mysqlStart) / 1_000_000;

        System.out.println(
                "MYSQL RESPONSE TIME -> "
                        + mysqlTime + " ms"
        );

        if (product.isEmpty()) {
            return null;
        }

        Product result = product.get();

        // Store product in Redis
        try {

            String json =
                    objectMapper.writeValueAsString(result);

            redis.setex(
                    key,
                    CACHE_TTL,
                    json
            );

            System.out.println(
                    "Stored Product " + id + " in Redis"
            );

        } catch (Exception e) {

            System.out.println(
                    "Could not store product in Redis."
            );
        }

        return result;
    }


    // ==========================================
    // GET ALL PRODUCTS
    // ==========================================

    public List<Product> getAllProducts() {

        String key = ALL_PRODUCTS_KEY;

        // Start timing Redis lookup
        long redisStart = System.nanoTime();

        try {

            String cachedProducts = redis.get(key);

            long redisTime =
                    (System.nanoTime() - redisStart) / 1_000_000;

            if (cachedProducts != null) {

                // Record cache hit
                cacheStatsService.recordHit();

                System.out.println(
                        "CACHE HIT -> All Products"
                );

                System.out.println(
                        "REDIS RESPONSE TIME -> "
                                + redisTime + " ms"
                );

                return objectMapper.readValue(
                        cachedProducts,
                        objectMapper.getTypeFactory()
                                .constructCollectionType(
                                        List.class,
                                        Product.class
                                )
                );
            }

            // Record cache miss
            cacheStatsService.recordMiss();

            System.out.println(
                    "CACHE MISS -> All Products"
            );

            System.out.println(
                    "REDIS LOOKUP TIME -> "
                            + redisTime + " ms"
            );

        } catch (Exception e) {

            // Redis failed, treat this request as a cache miss
            cacheStatsService.recordMiss();

            long redisTime =
                    (System.nanoTime() - redisStart) / 1_000_000;

            System.out.println(
                    "REDIS ERROR AFTER "
                            + redisTime
                            + " ms"
            );

            System.out.println(
                    "Redis unavailable. Reading all products from MySQL."
            );
        }

        // Start timing MySQL
        long mysqlStart = System.nanoTime();

        List<Product> products =
                productRepository.findAll();

        long mysqlTime =
                (System.nanoTime() - mysqlStart) / 1_000_000;

        System.out.println(
                "MYSQL RESPONSE TIME -> "
                        + mysqlTime + " ms"
        );

        // Store all products in Redis
        try {

            String json =
                    objectMapper.writeValueAsString(products);

            redis.setex(
                    key,
                    CACHE_TTL,
                    json
            );

            System.out.println(
                    "Stored All Products in Redis"
            );

        } catch (Exception e) {

            System.out.println(
                    "Could not store all products in Redis."
            );
        }

        return products;
    }


    // ==========================================
    // CREATE PRODUCT
    // ==========================================

    public Product createProduct(Product product) {

        Product savedProduct =
                productRepository.save(product);

        // Invalidate all-products cache
        evictAllProductsCache();

        return savedProduct;
    }


    // ==========================================
    // UPDATE PRODUCT
    // ==========================================

    public Product updateProduct(
            Long id,
            Product updatedProduct) {

        Optional<Product> existing =
                productRepository.findById(id);

        if (existing.isEmpty()) {
            return null;
        }

        Product product = existing.get();

        product.setName(
                updatedProduct.getName()
        );

        product.setDescription(
                updatedProduct.getDescription()
        );

        product.setCategory(
                updatedProduct.getCategory()
        );

        product.setPrice(
                updatedProduct.getPrice()
        );

        Product saved =
                productRepository.save(product);

        // Invalidate product and all-products cache
        evictCache(id);

        return saved;
    }


    // ==========================================
    // DELETE PRODUCT
    // ==========================================

    public boolean deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            return false;
        }

        productRepository.deleteById(id);

        // Invalidate product and all-products cache
        evictCache(id);

        return true;
    }


    // ==========================================
    // CACHE INVALIDATION
    // ==========================================

    private void evictCache(Long id) {

        String productKey =
                CACHE_PREFIX + id;

        try {

            redis.del(productKey);

            System.out.println(
                    "CACHE INVALIDATED -> "
                            + productKey
            );

            redis.del(ALL_PRODUCTS_KEY);

            System.out.println(
                    "CACHE INVALIDATED -> "
                            + ALL_PRODUCTS_KEY
            );

        } catch (Exception e) {

            System.out.println(
                    "Redis unavailable during cache invalidation."
            );
        }
    }


    // ==========================================
    // INVALIDATE ALL-PRODUCTS CACHE
    // ==========================================

    private void evictAllProductsCache() {

        try {

            redis.del(ALL_PRODUCTS_KEY);

            System.out.println(
                    "CACHE INVALIDATED -> "
                            + ALL_PRODUCTS_KEY
            );

        } catch (Exception e) {

            System.out.println(
                    "Redis unavailable during cache invalidation."
            );
        }
    }
}
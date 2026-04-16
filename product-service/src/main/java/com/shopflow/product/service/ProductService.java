package com.shopflow.product.service;

import com.shopflow.product.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    List<CategoryResponse> listCategories();

    void updateAvgRating(java.util.UUID productId, java.math.BigDecimal avgRating);

    ProductImageResponse uploadProductImage(UUID sellerId, UUID productId,
                                            org.springframework.web.multipart.MultipartFile file, boolean primary);

    ProductResponse toggleProductStatus(UUID sellerId, UUID productId,
                                        boolean active, String role);

    ProductResponse createProduct(UUID sellerId, ProductRequest request);

    ProductResponse getProduct(UUID productId);

    ProductResponse updateProduct(UUID sellerId, UUID productId, ProductRequest request);

    // role passed as String to avoid coupling the service interface
    // to a security-layer enum. Caller extracts role from JWT and
    // passes it in. "SELLER" can only delete own products.
    // "ADMIN" can delete any product.
    void deleteProduct(UUID userId, UUID productId, String role);

    Page<ProductResponse> listProducts(int page, int size);

    Page<ProductSearchResponse> searchProducts(String query, int page, int size);

    Page<ProductResponse> getProductsBySeller(UUID sellerId, int page, int size);

    void updateStock(UUID skuId, int quantity, StockUpdateRequest.StockOperation operation);

    SkuDetailsResponse getSkuDetails(UUID skuId);

    Page<ProductResponse> getAllProducts(Pageable pageable);
    void deleteProductByAdmin(UUID productId);

}
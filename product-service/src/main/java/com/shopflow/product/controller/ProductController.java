package com.shopflow.product.controller;

import com.shopflow.product.config.SecurityConfig;
import com.shopflow.product.dto.*;
import com.shopflow.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ------------------------------------------------------------------
    // PRODUCTS — WRITE
    // ------------------------------------------------------------------

    // POST /api/products
    // SELLER only. sellerId extracted from gateway-forwarded header,
    // never from the request body.
    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID sellerId = UUID.fromString(authentication.getName());

        log.info("Create product — sellerId: {}", sellerId);

        ProductResponse response = productService.createProduct(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /api/products/{id}
    // SELLER only. Ownership verified inside service layer.
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID sellerId = UUID.fromString(authentication.getName());

        log.info("Update product — productId: {} sellerId: {}", id, sellerId);

        ProductResponse response = productService.updateProduct(sellerId, id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/products/{id}
    // SELLER can delete own product. ADMIN can delete any product.
    // Role extracted from authentication authorities — the service
    // layer uses it to decide whether ownership check applies.
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        // Extract the role from the granted authority.
        // Authority is stored as "ROLE_SELLER" or "ROLE_ADMIN" —
        // strip the prefix before passing to the service layer.
        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        log.info("Delete product — productId: {} userId: {} role: {}", id, userId, role);

        productService.deleteProduct(userId, id, role);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // PRODUCTS — READ
    // ------------------------------------------------------------------

    // GET /api/products/{id}
    // Public. No authentication required.
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    // GET /api/products?page=0&size=20
    // Public. Returns paginated active products sorted by createdAt desc.
    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater") int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1") int size) {

        return ResponseEntity.ok(productService.listProducts(page, size));
    }

    // GET /api/products/search?q=leather+wallet&page=0&size=20
    // Public. Full-text search via Elasticsearch.
    // Mapped before /api/products/{id} in class but Spring MVC resolves
    // /search as a literal path before treating it as a path variable.
    @GetMapping("/products/search")
    public ResponseEntity<Page<ProductSearchResponse>> searchProducts(
            @RequestParam("q") String query,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater") int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1") int size) {

        log.info("Search products — query: '{}' page: {} size: {}", query, page, size);

        return ResponseEntity.ok(productService.searchProducts(query, page, size));
    }

    // GET /api/products/seller/{sellerId}?page=0&size=20
    // Public. Returns active products for a given seller storefront.
    @GetMapping("/products/seller/{sellerId}")
    public ResponseEntity<Page<ProductResponse>> getProductsBySeller(
            @PathVariable UUID sellerId,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater") int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1") int size) {

        return ResponseEntity.ok(productService.getProductsBySeller(sellerId, page, size));
    }

    // ------------------------------------------------------------------
    // CATEGORIES — READ
    // ------------------------------------------------------------------

    // GET /api/categories
    // Public. Returns all top-level categories.
    // Category write endpoints (POST/PUT/DELETE) are ADMIN-only and
    // will be added in admin-service or a CategoryController extension.
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(productService.listCategories());
    }

    // ------------------------------------------------------------------
    // INTERNAL — STOCK OPERATIONS
    // Called by order-service via Feign. Not exposed to external clients.
    // ------------------------------------------------------------------

    @PostMapping("/products/internal/update-stock")
    public ResponseEntity<Void> updateStock(
            @Valid @RequestBody StockUpdateRequest request) {

        log.info("Update stock — skuId: {} quantity: {} operation: {}",
                request.skuId(), request.quantity(), request.operation());

        productService.updateStock(request.skuId(), request.quantity(), request.operation());
        return ResponseEntity.ok().build();
    }


    // POST /api/products/{id}/images?primary=false
// SELLER only. Uploads an image to MinIO and saves a ProductImage row.
// Accepts multipart/form-data with a single file field named "file".
    @PostMapping(value = "/products/{id}/images",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> uploadProductImage(
            @PathVariable UUID id,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primary,
            Authentication authentication) {

        UUID sellerId = UUID.fromString(authentication.getName());

        log.info("Upload image — productId: {} sellerId: {} primary: {}",
                id, sellerId, primary);

        ProductImageResponse response =
                productService.uploadProductImage(sellerId, id, file, primary);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    // PATCH /api/products/{id}/status
// SELLER can deactivate/reactivate their own product.
// ADMIN can toggle any product.
// PATCH is correct here — partial update of one field,
// not a full resource replacement like PUT.
    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ProductResponse> toggleProductStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStatusRequest request,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        log.info("Toggle product status — productId: {} active: {} userId: {} role: {}",
                id, request.active(), userId, role);

        ProductResponse response = productService.toggleProductStatus(
                userId, id, request.active(), role);

        return ResponseEntity.ok(response);
    }


    // GET /api/products/internal/sku/{skuId}
    // Called by order-service via Feign to validate SKU exists
    // and enrich cart item with real product name and price.
    @GetMapping("/products/internal/sku/{skuId}")
    public ResponseEntity<SkuDetailsResponse> getSkuDetails(
            @PathVariable UUID skuId) {

        log.info("Internal — get SKU details skuId: {}", skuId);
        return ResponseEntity.ok(productService.getSkuDetails(skuId));
    }

    // ─── Admin Internal Endpoints ──────────────────────────────────────────────
// Called by admin-service via Feign only.
// No JWT required — whitelisted in SecurityConfig.

    // GET /api/products/internal/all
    @GetMapping("/products/internal/all")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Internal — get all products");
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // DELETE /api/products/internal/{productId}
// Admin hard delete — no ownership check applied
    @DeleteMapping("/products/internal/{productId}")
    public ResponseEntity<Void> deleteProductByAdmin(
            @PathVariable UUID productId) {

        log.info("Internal — admin delete product: {}", productId);
        productService.deleteProductByAdmin(productId);
        return ResponseEntity.noContent().build();
    }


}
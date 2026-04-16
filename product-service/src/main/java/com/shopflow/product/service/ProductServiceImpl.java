package com.shopflow.product.service;

import com.shopflow.product.client.SellerServiceClient;
import com.shopflow.product.document.ProductDocument;
import com.shopflow.product.dto.*;
import com.shopflow.product.entity.Product;
import com.shopflow.product.entity.ProductImage;
import com.shopflow.product.entity.ProductSku;
import com.shopflow.product.event.ProductLowStockEvent;
import com.shopflow.product.exception.*;
import com.shopflow.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSearchRepository productSearchRepository;
    private final SellerServiceClient sellerServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MinioService minioService;

    @Value("${app.kafka.topics.low-stock}")
    private String lowStockTopic;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(CategoryResponse::withChildren)
                .toList();
    }


    @Override
    @Transactional
    public void updateAvgRating(UUID productId, BigDecimal avgRating) {

        var product = productRepository.findByIdWithSkus(productId)
                .orElseThrow(() -> new com.shopflow.product.exception.ResourceNotFoundException(
                        "Product not found for rating update: " + productId
                ));

        product.setAvgRating(avgRating);
        productRepository.save(product);

        // Re-index in Elasticsearch so search results reflect
        // the updated rating immediately after the event is processed.
        indexProduct(product);

        log.info("avgRating updated — productId: {} newRating: {}", productId, avgRating);
    }


    @Override
    @Transactional
    public ProductImageResponse uploadProductImage(UUID sellerId, UUID productId,
                                                   MultipartFile file, boolean primary) {

        // Verify product exists and belongs to this seller
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new com.shopflow.product.exception.ResourceNotFoundException(
                        "Product not found: " + productId
                ));

        if (!product.getSellerId().equals(sellerId)) {
            throw new com.shopflow.product.exception.UnauthorizedProductAccessException(
                    "Seller does not own this product"
            );
        }

        // Validate file is not empty
        if (file == null || file.isEmpty()) {
            throw new com.shopflow.product.exception.FileStorageException(
                    "Image file must not be empty"
            );
        }

        // Validate content type — only images accepted
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new com.shopflow.product.exception.FileStorageException(
                    "Only image files are accepted. Received: " + contentType
            );
        }

        // If this image is being set as primary, clear the existing primary first.
        // clearPrimaryByProductId is a bulk UPDATE — no need to load all images.
        if (primary) {
            productImageRepository.clearPrimaryByProductId(productId);
        }

        // Upload to MinIO under the products/ folder
        String objectKey = minioService.uploadFile(file, "products");

        // Persist the image record
        var image = ProductImage.builder()
                .product(product)
                .imageUrl(objectKey)
                .primary(primary)
                .build();

        var saved = productImageRepository.save(image);

        // Re-index the product in Elasticsearch — image changes do not
        // affect the search document directly but keeping the index
        // fresh is consistent with the rest of the update flow.
        // Fetch with SKUs to satisfy indexProduct's minPrice calculation.
        var productWithSkus = productRepository.findByIdWithSkus(productId)
                .orElse(product);
        indexProduct(productWithSkus);

        log.info("Image uploaded — productId: {} imageId: {} primary: {}",
                productId, saved.getId(), primary);

        return ProductImageResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductResponse toggleProductStatus(UUID sellerId, UUID productId,
                                               boolean active, String role) {

        var product = productRepository.findByIdWithSkus(productId)
                .orElseThrow(() -> new com.shopflow.product.exception.ResourceNotFoundException(
                        "Product not found: " + productId
                ));

        // SELLER can only toggle their own product.
        // ADMIN can toggle any product.
        boolean isSeller = "SELLER".equals(role);
        if (isSeller && !product.getSellerId().equals(sellerId)) {
            throw new com.shopflow.product.exception.UnauthorizedProductAccessException(
                    "Seller does not own this product"
            );
        }

        // No-op guard — if the product is already in the requested
        // state, skip the write and return the current state immediately.
        if (product.isActive() == active) {
            log.info("Product status unchanged — productId: {} active: {}",
                    productId, active);
            return ProductResponse.from(product);
        }

        product.setActive(active);
        var saved = productRepository.save(product);

        // Re-index in Elasticsearch so inactive products are
        // filtered out of search results immediately.
        indexProduct(saved);

        log.info("Product status toggled — productId: {} active: {} by userId: {} role: {}",
                productId, active, sellerId, role);

        return ProductResponse.from(saved);
    }


    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProductResponse createProduct(UUID sellerId, ProductRequest request) {

        // Validate seller exists and is APPROVED via Feign
        var sellerResponse = sellerServiceClient.getSellerById(sellerId);
        if (!sellerResponse.isApproved()) {
            throw new SellerNotApprovedException(
                    "Seller is not approved to list products"
            );
        }

        // Validate category exists
        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.categoryId()
                ));

        // Build product entity
        var product = Product.builder()
                .sellerId(sellerId)
                .category(category)
                .name(request.name())
                .description(request.description())
                .brand(request.brand())
                .active(true)
                .build();

        // Build and attach SKUs
        List<ProductSku> skus = request.skus().stream()
                .map(skuRequest -> ProductSku.builder()
                        .product(product)
                        .skuCode(skuRequest.skuCode())
                        .variantName(skuRequest.variantName())
                        .price(skuRequest.price())
                        .stockQty(skuRequest.stockQty())
                        .lowStockThreshold(
                                skuRequest.lowStockThreshold() != null
                                        ? skuRequest.lowStockThreshold()
                                        : 5
                        )
                        .build())
                .toList();

        product.getSkus().addAll(skus);

        var saved = productRepository.save(product);

        // Index in Elasticsearch
        indexProduct(saved);

        log.info("Product created: {} by seller: {}", saved.getId(), sellerId);

        return ProductResponse.from(saved);
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        var product = productRepository.findByIdWithSkus(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId
                ));
        return ProductResponse.from(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findByActive(true, pageable)
                .map(ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsBySeller(UUID sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findBySellerIdAndActive(sellerId, true, pageable)
                .map(ProductResponse::from);
    }

    // ------------------------------------------------------------------
    // SEARCH (Elasticsearch)
    // ------------------------------------------------------------------

    @Override
    public Page<ProductSearchResponse> searchProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productSearchRepository.searchByText(query, pageable)
                .map(ProductSearchResponse::from);
    }

    // ------------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID sellerId, UUID productId, ProductRequest request) {

        var product = productRepository.findByIdWithSkus(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId
                ));

        // Seller can only update their own product
        if (!product.getSellerId().equals(sellerId)) {
            throw new UnauthorizedProductAccessException(
                    "Seller does not own this product"
            );
        }

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.categoryId()
                ));

        // Update scalar fields
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setCategory(category);

        // Replace SKUs — remove existing, add new ones from request.
        // orphanRemoval = true on the entity handles the deletes.
        product.getSkus().clear();

        productRepository.saveAndFlush(product);

        List<ProductSku> updatedSkus = request.skus().stream()
                .map(skuRequest -> ProductSku.builder()
                        .product(product)
                        .skuCode(skuRequest.skuCode())
                        .variantName(skuRequest.variantName())
                        .price(skuRequest.price())
                        .stockQty(skuRequest.stockQty())
                        .lowStockThreshold(
                                skuRequest.lowStockThreshold() != null
                                        ? skuRequest.lowStockThreshold()
                                        : 5
                        )
                        .build())
                .toList();
        product.getSkus().addAll(updatedSkus);

        var saved = productRepository.saveAndFlush(product);

        // Re-index in Elasticsearch with updated data
        indexProduct(saved);

        log.info("Product updated: {} by seller: {}", productId, sellerId);

        return ProductResponse.from(saved);
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteProduct(UUID userId, UUID productId, String role) {

        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId
                ));

        // SELLER can only delete their own product.
        // ADMIN can delete any product.
        boolean isSeller = "SELLER".equals(role);
        if (isSeller && !product.getSellerId().equals(userId)) {
            throw new UnauthorizedProductAccessException(
                    "Seller does not own this product"
            );
        }

        productRepository.delete(product);

        // Remove from Elasticsearch index
        productSearchRepository.deleteById(productId.toString());

        log.info("Product deleted: {} by userId: {} role: {}", productId, userId, role);
    }

    // ------------------------------------------------------------------
    // STOCK — called by order-service via Feign
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public void updateStock(UUID skuId, int quantity,
                            StockUpdateRequest.StockOperation operation) {

        var sku = productSkuRepository.findByIdForUpdate(skuId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SKU not found: " + skuId
                ));

        if (operation == StockUpdateRequest.StockOperation.DECREMENT) {
            if (sku.getStockQty() < quantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for SKU: " + skuId +
                                ". Requested: " + quantity +
                                ", Available: " + sku.getStockQty()
                );
            }
            sku.setStockQty(sku.getStockQty() - quantity);
            productSkuRepository.save(sku);
            checkAndFireLowStockEvent(sku);
        } else {
            sku.setStockQty(sku.getStockQty() + quantity);
            productSkuRepository.save(sku);
        }

        log.info("Stock updated — skuId: {} quantity: {} operation: {} remaining: {}",
                skuId, quantity, operation, sku.getStockQty());
    }

    @Override
    public SkuDetailsResponse getSkuDetails(UUID skuId) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SKU not found: " + skuId));

        Product product = sku.getProduct();

        return SkuDetailsResponse.builder()
                .skuId(sku.getId())
                .productId(product.getId())
                .sellerId(product.getSellerId())
                .productName(product.getName())
                .price(sku.getPrice())
                .stockQty(sku.getStockQty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::from);
    }

    @Override
    @Transactional
    public void deleteProductByAdmin(UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        productRepository.delete(product);

        // Remove from Elasticsearch index
        productSearchRepository.deleteById(productId.toString());

        log.info("Product deleted by admin — productId: {}", productId);
    }



    // ------------------------------------------------------------------
    // PRIVATE HELPERS
    // ------------------------------------------------------------------

    // Builds and saves a ProductDocument to Elasticsearch.
    // Called after create and update to keep the index in sync.
    private void indexProduct(Product product) {
        BigDecimal minPrice = product.getSkus().stream()
                .map(ProductSku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        var document = new ProductDocument(
                product.getId().toString(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getCategory().getId().toString(),
                product.getSellerId().toString(),
                product.getAvgRating(),
                minPrice,
                product.isActive()
        );

        productSearchRepository.save(document);
    }

    // Fires product.low.stock Kafka event if any SKU for
    // this product has crossed the low stock threshold.
    private void checkAndFireLowStockEvent(ProductSku sku) {

        if (sku.getStockQty() <= sku.getLowStockThreshold()) {

            //  No extra DB call
            var product = sku.getProduct();

            if (product == null) {
                log.warn("Could not fire low-stock event — product not found for SKU: {}",
                        sku.getId());
                return;
            }

            // Fetch seller details
            var seller = sellerServiceClient.getSellerDetails(product.getSellerId());

            // Build event
            var event = new ProductLowStockEvent(
                    product.getId(),
                    product.getSellerId(),
                    seller.getEmail(),
                    seller.getFullName(),
                    product.getName(),
                    sku.getSkuCode(),
                    sku.getStockQty(),
                    sku.getLowStockThreshold()
            );



            //  Send EVENT
            kafkaTemplate.send(lowStockTopic, product.getId().toString(), event);

            log.info("Low-stock event fired — productId: {} skuId: {} stock: {}",
                    product.getId(), sku.getId(), sku.getStockQty());
            log.info("Seller details → email: {}, name: {}",
                    seller.getEmail(), seller.getFullName());
        }
    }
}
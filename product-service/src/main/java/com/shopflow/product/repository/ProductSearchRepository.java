package com.shopflow.product.repository;

import com.shopflow.product.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDocument, String> {

    // Full-text search across name and description.
    // Boosting name (^3) means a match in the product name ranks
    // higher than the same match found only in the description.
    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["name^3", "description"],
                      "type": "best_fields",
                      "fuzziness": "AUTO"
                    }
                  }
                ],
                "filter": [
                  { "term": { "is_active": true } }
                ]
              }
            }
            """)
    Page<ProductDocument> searchByText(String query, Pageable pageable);

    // Exact-match filter by brand — used in sidebar filters.
    Page<ProductDocument> findByBrandAndActive(String brand, boolean active, Pageable pageable);

    // Exact-match filter by category — used in category browse pages.
    Page<ProductDocument> findByCategoryIdAndActive(
            String categoryId, boolean active, Pageable pageable);

    // Exact-match filter by seller — used on seller storefront pages.
    Page<ProductDocument> findBySellerIdAndActive(
            String sellerId, boolean active, Pageable pageable);
}
package com.shopflow.product.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;

@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
public record ProductDocument(

        @Id
        String id,

        @Field(type = FieldType.Text, analyzer = "english")
        String name,

        @Field(type = FieldType.Text, analyzer = "english")
        String description,

        @Field(type = FieldType.Keyword)
        String brand,

        @Field(type = FieldType.Keyword, name = "category_id")
        String categoryId,

        @Field(type = FieldType.Keyword, name = "seller_id")
        String sellerId,

        @Field(type = FieldType.Double, name = "avg_rating")
        BigDecimal avgRating,

        @Field(type = FieldType.Double, name = "min_price")
        BigDecimal minPrice,

        @Field(type = FieldType.Boolean, name = "is_active")
        boolean active

) {}
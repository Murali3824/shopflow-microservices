package com.shopflow.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Null means this is a top-level category (e.g. "Electronics")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    // Children loaded lazily — never auto-traverse the full tree
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(name = "name", nullable = false)
    private String name;

    // URL-safe identifier e.g. "mens-footwear" — used in search filters and routing
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;
}
-- =============================================================
-- V1__create_product_tables.sql
-- ShopFlow product-service initial schema
-- =============================================================

-- -------------------------------------------------------------
-- CATEGORIES
-- Must be created before products because products.category_id
-- references it. Self-referencing via parent_id.
-- -------------------------------------------------------------
CREATE TABLE categories (
                            id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                            parent_id   UUID            REFERENCES categories(id) ON DELETE RESTRICT,
                            name        VARCHAR(100)    NOT NULL,
                            slug        VARCHAR(100)    NOT NULL,

                            CONSTRAINT uq_categories_slug UNIQUE (slug)
);

-- -------------------------------------------------------------
-- PRODUCTS
-- seller_id is a reference ID only — no FK to seller_db.
-- category_id is a real FK — both tables live in product_db.
-- -------------------------------------------------------------
CREATE TABLE products (
                          id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                          seller_id       UUID            NOT NULL,
                          category_id     UUID            NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
                          name            VARCHAR(255)    NOT NULL,
                          description     TEXT,
                          brand           VARCHAR(100),
                          is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                          avg_rating      NUMERIC(3, 2),
                          created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- -------------------------------------------------------------
-- PRODUCT_SKUS
-- Each SKU is one purchasable variant of a product.
-- sku_code must be globally unique across all products.
-- -------------------------------------------------------------
CREATE TABLE product_skus (
                              id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                              product_id          UUID            NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                              sku_code            VARCHAR(100)    NOT NULL,
                              variant_name        VARCHAR(255),
                              price               NUMERIC(10, 2)  NOT NULL,
                              stock_qty           INT             NOT NULL DEFAULT 0,
                              low_stock_threshold INT             NOT NULL DEFAULT 5,

                              CONSTRAINT uq_product_skus_sku_code     UNIQUE (sku_code),
                              CONSTRAINT chk_product_skus_price       CHECK (price >= 0),
                              CONSTRAINT chk_product_skus_stock_qty   CHECK (stock_qty >= 0),
                              CONSTRAINT chk_product_skus_threshold   CHECK (low_stock_threshold >= 0)
);

-- -------------------------------------------------------------
-- PRODUCT_IMAGES
-- image_url stored as TEXT — MinIO URLs can be long.
-- Only one image per product should have is_primary = true.
-- Enforced at service layer, not via partial unique index,
-- to keep swap-primary logic simple.
-- -------------------------------------------------------------
CREATE TABLE product_images (
                                id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                image_url   TEXT        NOT NULL,
                                is_primary  BOOLEAN     NOT NULL DEFAULT FALSE
);

-- =============================================================
-- INDEXES
-- =============================================================

-- products: seller_id filter — list all products by a seller
CREATE INDEX idx_products_seller_id     ON products(seller_id);

-- products: category_id filter — browse by category
CREATE INDEX idx_products_category_id   ON products(category_id);

-- products: active filter — almost every query excludes inactive products
CREATE INDEX idx_products_is_active     ON products(is_active);

-- products: composite — seller's active product listings (most common admin/seller query)
CREATE INDEX idx_products_seller_active ON products(seller_id, is_active);

-- product_skus: product_id — fetch all SKUs for a product
CREATE INDEX idx_product_skus_product_id ON product_skus(product_id);

-- product_images: product_id — fetch all images for a product
CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- categories: parent_id — fetch all children of a category
CREATE INDEX idx_categories_parent_id   ON categories(parent_id);
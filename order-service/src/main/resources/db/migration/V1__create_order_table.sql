-- ============================================================
-- V1__create_order_tables.sql
-- order-service schema — order_db
-- ============================================================

-- ------------------------------------------------------------
-- ENUMS AS VARCHAR CHECK CONSTRAINTS
-- Reason: Native PostgreSQL ENUMs conflict with Hibernate's
-- default handling (same issue fixed in auth-service via V2
-- migration). Using VARCHAR + CHECK is cleaner and avoids
-- that problem entirely from the start.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- TABLE: orders
-- Stores one row per placed order.
--
-- address_snapshot: JSONB — copy of the delivery address at
--   order time. If user later edits/deletes address, the
--   original delivery address is preserved. Real production pattern.
--
-- coupon_code: nullable — only set if user applied a coupon.
--
-- status: PENDING on creation. Moves to CONFIRMED after
--   payment.completed Kafka event is consumed. Then SHIPPED
--   when seller marks it. Then DELIVERED. Or CANCELLED.
--
-- payment_method: which gateway the user chose at checkout.
--   RAZORPAY or STRIPE. Stored here so payment-service knows
--   which SDK to use when it receives order.placed event.
--
-- total_amount: final amount after discount.
-- discount_amount: how much the coupon saved (0 if no coupon).
-- ------------------------------------------------------------
CREATE TABLE orders
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID         NOT NULL,
    address_snapshot JSONB       NOT NULL,
    coupon_code     VARCHAR(50),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_order_status
            CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_amount    NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_method  VARCHAR(20)  NOT NULL
        CONSTRAINT chk_payment_method
            CHECK (payment_method IN ('RAZORPAY', 'STRIPE')),
    placed_at       TIMESTAMP    NOT NULL DEFAULT now()
);

-- Index: most common query — fetch all orders for a user
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Index: admin and seller queries filter by status
CREATE INDEX idx_orders_status ON orders (status);

-- ------------------------------------------------------------
-- TABLE: order_items
-- One row per product SKU per order.
--
-- product_name + unit_price: snapshots at order time.
--   If seller later changes the product name or price, the
--   order history still shows what the user actually paid.
--   Same principle as address_snapshot on orders table.
--
-- seller_id: stored here so seller-service can query
--   "all items in orders assigned to me" without joining
--   across services.
--
-- subtotal: unit_price × quantity, pre-calculated and stored.
--   Avoids recalculating on every read. Consistent with what
--   was charged.
-- ------------------------------------------------------------
CREATE TABLE order_items
(
    id           UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id     UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    sku_id       UUID           NOT NULL,
    seller_id    UUID           NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    subtotal     NUMERIC(12, 2) NOT NULL
);

-- Index: fetch all items belonging to an order (used on every order detail page)
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- Index: seller queries — "show me all order items assigned to my store"
CREATE INDEX idx_order_items_seller_id ON order_items (seller_id);

-- ------------------------------------------------------------
-- TABLE: order_status_history
-- Append-only log of every status transition an order goes
-- through. Never updated — only inserted.
--
-- Every time order status changes (PENDING → CONFIRMED etc.)
-- we insert a new row here. Gives a full audit trail.
--
-- note: free-text field. Used for things like:
--   - "Payment confirmed via Razorpay"
--   - "Tracking number: FEDEX123456"
--   - "Cancelled by user"
-- ------------------------------------------------------------
CREATE TABLE order_status_history
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id   UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL
        CONSTRAINT chk_history_status
            CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    note       TEXT,
    changed_at TIMESTAMP   NOT NULL DEFAULT now()
);

-- Index: always queried by order_id to show the timeline
CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);

-- ------------------------------------------------------------
-- TABLE: return_requests
-- One return request per order item. User raises it after
-- delivery. Admin approves or rejects.
--
-- order_item_id: return is at item level, not order level.
--   User can return one item from a multi-item order.
--
-- status flow:
--   REQUESTED → APPROVED → REFUNDED  (happy path)
--   REQUESTED → REJECTED             (admin rejects)
--
-- When admin approves: order-service fires return.approved
--   Kafka event → payment-service triggers the actual refund.
--   Status moves to REFUNDED after payment-service confirms.
-- ------------------------------------------------------------
CREATE TABLE return_requests
(
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id      UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    order_item_id UUID        NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    reason        TEXT        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
        CONSTRAINT chk_return_status
            CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'REFUNDED')),
    requested_at  TIMESTAMP   NOT NULL DEFAULT now()
);

-- Index: admin views all return requests, often filtered by status
CREATE INDEX idx_return_requests_order_id ON return_requests (order_id);
CREATE INDEX idx_return_requests_status ON return_requests (status);
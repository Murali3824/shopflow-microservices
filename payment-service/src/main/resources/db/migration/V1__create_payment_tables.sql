-- =====================================================================
-- V1__create_payment_tables.sql
-- payment-service schema
-- Tables: payments, refunds, seller_earnings
-- =====================================================================

-- =====================================================================
-- ENUM TYPES
-- Using VARCHAR instead of PostgreSQL native ENUM types.
-- Reason: Native ENUMs conflict with Hibernate's default handling
-- and require ALTER TYPE to add new values later. VARCHAR with
-- application-level validation is safer and easier to evolve.
-- =====================================================================

-- =====================================================================
-- TABLE: payments
--
-- One row per order payment attempt.
--
-- gateway: RAZORPAY / STRIPE / COD
--   COD rows have NULL gateway_order_id and gateway_payment_id
--   because no gateway is involved.
--
-- gateway_order_id: The order ID returned by Razorpay/Stripe when
--   we create a payment intent. Needed to track the session.
--
-- gateway_payment_id: The actual payment ID confirmed by the gateway
--   after the user pays. This is CRITICAL — we need this exact ID
--   to call the refund API later. NULL until payment succeeds.
--   For COD, stays NULL — refund is a DB record only.
--
-- status: PENDING → SUCCESS / FAILED
--   COD orders start PENDING and move to SUCCESS on delivery.
--   Razorpay/Stripe move to SUCCESS via verify endpoint or webhook.
--
-- idempotency_key: One unique key per order. Prevents duplicate
--   payment records if the frontend retries the initiate call.
--   Format we will use: "order-{orderId}"
--
-- paid_at: NULL until status becomes SUCCESS.
-- =====================================================================

CREATE TABLE payments
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id            UUID         NOT NULL,
    user_id             UUID         NOT NULL,
    gateway             VARCHAR(20)  NOT NULL,
    gateway_order_id    VARCHAR(255),
    gateway_payment_id  VARCHAR(255),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    amount              DECIMAL(12, 2) NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- Index: order lookups are the most common query pattern
-- (get payment by orderId to check status, to trigger refund)
CREATE INDEX idx_payments_order_id ON payments (order_id);

-- Index: user payment history listing
CREATE INDEX idx_payments_user_id ON payments (user_id);

-- =====================================================================
-- TABLE: refunds
--
-- One row per refund attempt.
--
-- payment_id: FK to payments. We need the gateway_payment_id from
--   that row to call the Razorpay/Stripe refund API.
--
-- return_request_id: Reference ID from order-service. No FK because
--   cross-service joins are not allowed — this is a reference only.
--
-- amount: Full refund only (as per project scope — no partial refunds).
--
-- gateway_refund_id: The refund ID returned by Razorpay/Stripe.
--   Stored for audit and to check refund status later.
--   NULL for COD refunds — those are DB records only, no gateway call.
--
-- status: INITIATED → COMPLETED / FAILED
-- =====================================================================

CREATE TABLE refunds
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    payment_id          UUID         NOT NULL REFERENCES payments (id),
    return_request_id   UUID         NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL,
    gateway_refund_id   VARCHAR(255),
    status              VARCHAR(20)  NOT NULL DEFAULT 'INITIATED',
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- Index: look up refunds by payment (used when checking refund status)
CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);

-- =====================================================================
-- TABLE: seller_earnings
--
-- One row per ORDER_ITEM delivered (not per order).
-- Reason: A single order can contain items from multiple sellers.
-- Each seller earns independently on their own items.
--
-- seller_id: Reference to seller-service. No FK — cross-service
--   reference only.
--
-- order_id / order_item_id: References to order-service. Same rule —
--   reference IDs only, no cross-DB FK.
--
-- gross_amount: The full item subtotal (unit_price × quantity).
--
-- commission_amount: gross_amount × (commission_rate / 100)
--   The platform keeps this.
--
-- net_earning: gross_amount - commission_amount
--   What the seller actually earns.
--
-- commission_rate: Snapshot of the rate AT THE TIME of delivery.
--   Stored here so future rate changes don't alter historical records.
--   This is the same snapshot pattern used for address and price
--   in order-service.
-- =====================================================================

CREATE TABLE seller_earnings
(
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    seller_id         UUID          NOT NULL,
    order_id          UUID          NOT NULL,
    order_item_id     UUID          NOT NULL,
    gross_amount      DECIMAL(12, 2) NOT NULL,
    commission_rate   DECIMAL(5, 2)  NOT NULL,
    commission_amount DECIMAL(12, 2) NOT NULL,
    net_earning       DECIMAL(12, 2) NOT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT now()
);

-- Index: seller earnings dashboard — fetch all earnings for a seller
CREATE INDEX idx_seller_earnings_seller_id ON seller_earnings (seller_id);

-- Index: look up earnings triggered by a specific order
-- (used when order.delivered event arrives to avoid duplicates)
CREATE INDEX idx_seller_earnings_order_id ON seller_earnings (order_id);
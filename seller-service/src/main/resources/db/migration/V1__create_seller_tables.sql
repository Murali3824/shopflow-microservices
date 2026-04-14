CREATE TABLE sellers
(
    id               UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID           NOT NULL UNIQUE,
    business_name    VARCHAR(255)   NOT NULL,
    gst_number       VARCHAR(20)    NOT NULL UNIQUE,
    phone            VARCHAR(15)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    commission_rate  DECIMAL(5, 2)  NOT NULL DEFAULT 10.0,
    store_name       VARCHAR(255),
    store_description TEXT,
    logo_url         TEXT,
    created_at       TIMESTAMP      NOT NULL
);

CREATE TABLE coupons
(
    id              UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    seller_id       UUID            NOT NULL,
    code            VARCHAR(50)     NOT NULL UNIQUE,
    discount_type   VARCHAR(10)     NOT NULL,
    discount_value  DECIMAL(10, 2)  NOT NULL,
    valid_until     TIMESTAMP,
    usage_limit     INTEGER,
    times_used      INTEGER         NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_coupon_seller FOREIGN KEY (seller_id) REFERENCES sellers (id)
);
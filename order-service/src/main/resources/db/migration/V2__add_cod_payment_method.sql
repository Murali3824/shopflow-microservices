ALTER TABLE orders
DROP CONSTRAINT chk_payment_method;

ALTER TABLE orders
    ADD CONSTRAINT chk_payment_method
        CHECK (payment_method IN ('RAZORPAY', 'STRIPE', 'COD'));
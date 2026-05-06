CREATE TABLE reviews (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id UUID NOT NULL,
                         product_id UUID NOT NULL,
                         order_id UUID NOT NULL,
                         rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                         title VARCHAR(255) NOT NULL,
                         body TEXT NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT now(),

                         CONSTRAINT uq_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
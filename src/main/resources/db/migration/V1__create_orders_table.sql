CREATE SEQUENCE orders_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE orders (
    id                BIGINT PRIMARY KEY DEFAULT nextval('orders_SEQ'),
    customer_id       VARCHAR(64)     NOT NULL,
    amount_usd        NUMERIC(19, 2)  NOT NULL,
    target_currency   VARCHAR(3)      NOT NULL,
    converted_amount  NUMERIC(19, 2)  NOT NULL,
    exchange_rate     NUMERIC(19, 6)  NOT NULL,
    status            VARCHAR(20)     NOT NULL,
    created_at        TIMESTAMPTZ     NOT NULL
);

ALTER SEQUENCE orders_SEQ OWNED BY orders.id;

CREATE INDEX idx_orders_customer_id ON orders (customer_id);

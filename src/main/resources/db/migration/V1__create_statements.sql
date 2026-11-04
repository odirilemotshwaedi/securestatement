-- Creates the transaction table that backs the Transaction JPA entity.
-- 'transaction' matches the Hibernate-generated name (SpringPhysicalNamingStrategy).
-- IF NOT EXISTS makes this safe to run against a database that Hibernate
-- already bootstrapped with ddl-auto=update.
CREATE TABLE IF NOT EXISTS transaction (
    id          BIGSERIAL    PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50)  NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(10)  NOT NULL,
    location    VARCHAR(100),
    timestamp   TIMESTAMP    NOT NULL
);

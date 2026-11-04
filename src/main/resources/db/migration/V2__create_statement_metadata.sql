-- Creates the statement_metadata table that backs the StatementMetadata JPA entity.
-- Column names follow Hibernate's SpringPhysicalNamingStrategy (camelCase -> snake_case).
-- IF NOT EXISTS makes this safe to run against a database that Hibernate
-- already bootstrapped with ddl-auto=update.
CREATE TABLE IF NOT EXISTS statement_metadata (
    id           BIGSERIAL     PRIMARY KEY,
    customer_id  VARCHAR(255),
    file_path    VARCHAR(255),
    generated_at TIMESTAMP
);

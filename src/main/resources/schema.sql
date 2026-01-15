-- H2 In-Memory Database Schema for Transaction Management
-- This schema is executed on application startup

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    timestamp TIMESTAMP NOT NULL,
    
    -- Unique constraint for duplicate detection
    CONSTRAINT uk_transaction_content UNIQUE (amount, type, category, description)
);

-- Index for faster timestamp-based queries (pagination)
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp DESC);

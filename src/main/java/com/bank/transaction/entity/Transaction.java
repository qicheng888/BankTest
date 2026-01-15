package com.bank.transaction.entity;

import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transaction Entity
 * 
 * Represents a financial transaction in the banking system.
 * This is the core domain entity for the transaction management system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    /**
     * Unique identifier for the transaction
     */
    private String id;

    /**
     * Transaction amount in the base currency
     * Uses BigDecimal for precise monetary calculations
     */
    private BigDecimal amount;

    /**
     * Type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER)
     */
    private TransactionType type;

    /**
     * Category of the transaction for classification purposes
     */
    private TransactionCategory category;

    /**
     * Optional description or notes about the transaction
     */
    private String description;

    /**
     * Timestamp when the transaction was created/occurred
     */
    private LocalDateTime timestamp;

    /**
     * Generates a unique hash for duplicate detection
     * Using JDK 15+ Text Block style formatting (kept inline for simplicity)
     */
    public String generateDuplicateHash() {
        return "%s_%s_%s_%s".formatted(
                amount != null ? amount.stripTrailingZeros().toPlainString() : "",
                type != null ? type.name() : "",
                category != null ? category.name() : "",
                description != null ? description.trim().toLowerCase() : "");
    }

    /**
     * Uses JDK 16+ Pattern Matching for instanceof
     */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Transaction that && Objects.equals(id, that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

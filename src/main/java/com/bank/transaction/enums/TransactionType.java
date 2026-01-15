package com.bank.transaction.enums;

/**
 * Transaction Type Enumeration
 * 
 * Represents the type of financial transaction:
 * - DEPOSIT: Money added to account
 * - WITHDRAWAL: Money taken from account
 * - TRANSFER: Money moved between accounts
 */
public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER("Transfer");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

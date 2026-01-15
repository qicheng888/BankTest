package com.bank.transaction.exception;

/**
 * Duplicate Transaction Exception
 * 
 * Thrown when attempting to create a transaction that appears to be a
 * duplicate.
 * Uses JDK 17+ sealed class pattern - extends sealed TransactionException.
 */
public final class DuplicateTransactionException extends TransactionException {

    private final String duplicateHash;

    public DuplicateTransactionException(String message) {
        super(message);
        this.duplicateHash = null;
    }

    public DuplicateTransactionException(String message, String duplicateHash) {
        super(message);
        this.duplicateHash = duplicateHash;
    }

    public String duplicateHash() {
        return duplicateHash;
    }
}

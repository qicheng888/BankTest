package com.bank.transaction.exception;

/**
 * Transaction Not Found Exception
 * 
 * Thrown when attempting to access a transaction that doesn't exist.
 * Uses JDK 17+ sealed class pattern - extends sealed TransactionException.
 */
public final class TransactionNotFoundException extends TransactionException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("交易不存在: %s".formatted(transactionId));
        this.transactionId = transactionId;
    }

    public String transactionId() {
        return transactionId;
    }
}

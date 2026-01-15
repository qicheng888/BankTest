package com.bank.transaction.exception;

/**
 * Transaction Exception (Sealed Class)
 * 
 * JDK 17+ sealed class providing a restricted exception hierarchy.
 * Only TransactionNotFoundException and DuplicateTransactionException can extend this class.
 */
public sealed
abstract class TransactionException
        extends RuntimeException
permits TransactionNotFoundException, DuplicateTransactionException
{

    protected TransactionException(String message) {
        super(message);
    }
}

package com.bank.transaction.service;

import com.bank.transaction.dto.PageResponse;
import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.dto.TransactionResponse;

/**
 * Transaction Service Interface
 * 
 * Defines the business operations for transaction management.
 */
public interface TransactionService {

    /**
     * Create a new transaction
     * 
     * @param request the transaction creation request
     * @return the created transaction
     */
    TransactionResponse createTransaction(TransactionRequest request);

    /**
     * Get a transaction by ID
     * 
     * @param id the transaction ID
     * @return the transaction
     */
    TransactionResponse getTransaction(String id);

    /**
     * Get all transactions with pagination
     * 
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of transactions
     */
    PageResponse<TransactionResponse> getAllTransactions(int page, int size);

    /**
     * Update a transaction
     * 
     * @param id      the transaction ID
     * @param request the update request
     * @return the updated transaction
     */
    TransactionResponse updateTransaction(String id, TransactionRequest request);

    /**
     * Delete a transaction
     * 
     * @param id the transaction ID
     */
    void deleteTransaction(String id);
}

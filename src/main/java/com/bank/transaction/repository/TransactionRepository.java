package com.bank.transaction.repository;

import com.bank.transaction.entity.Transaction;
import com.bank.transaction.mapper.TransactionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository
 * 
 * Delegates all database operations to MyBatis TransactionMapper.
 * Implements the same interface contract as the original ConcurrentHashMap
 * implementation.
 */
@Repository
public class TransactionRepository {

    private final TransactionMapper transactionMapper;

    public TransactionRepository(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    /**
     * Save a new transaction
     * 
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    public Transaction save(Transaction transaction) {
        if (transactionMapper.existsById(transaction.getId())) {
            transactionMapper.update(transaction);
        } else {
            transactionMapper.insert(transaction);
        }
        return transaction;
    }

    /**
     * Find a transaction by ID
     * 
     * @param id the transaction ID
     * @return Optional containing the transaction if found
     */
    public Optional<Transaction> findById(String id) {
        return transactionMapper.findById(id);
    }

    /**
     * Find all transactions
     * 
     * @return list of all transactions
     */
    public List<Transaction> findAll() {
        return transactionMapper.findAll();
    }

    /**
     * Find transactions with pagination
     * 
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of transactions
     */
    public List<Transaction> findAllPaginated(int page, int size) {
        int offset = page * size;
        return transactionMapper.findAllPaginated(offset, size);
    }

    /**
     * Get total count of transactions
     * 
     * @return total number of transactions
     */
    public long count() {
        return transactionMapper.count();
    }

    /**
     * Delete a transaction by ID
     * 
     * @param id the transaction ID
     * @return true if deleted, false if not found
     */
    public boolean deleteById(String id) {
        return transactionMapper.deleteById(id) > 0;
    }

    /**
     * Check if a transaction exists by ID
     * 
     * @param id the transaction ID
     * @return true if exists
     */
    public boolean existsById(String id) {
        return transactionMapper.existsById(id);
    }

    /**
     * Check if a potential duplicate exists
     * 
     * @param transaction the transaction to check
     * @return true if a duplicate exists
     */
    public boolean existsDuplicate(Transaction transaction) {
        return transactionMapper.existsDuplicate(
                transaction.getAmount(),
                transaction.getType() != null ? transaction.getType().name() : null,
                transaction.getCategory() != null ? transaction.getCategory().name() : null,
                transaction.getDescription());
    }

    /**
     * Check if a potential duplicate exists (excluding a specific ID)
     * Used during update operations
     * 
     * @param transaction the transaction to check
     * @param excludeId   the ID to exclude from duplicate check
     * @return true if a duplicate exists
     */
    public boolean existsDuplicateExcluding(Transaction transaction, String excludeId) {
        return transactionMapper.existsDuplicateExcluding(
                transaction.getAmount(),
                transaction.getType() != null ? transaction.getType().name() : null,
                transaction.getCategory() != null ? transaction.getCategory().name() : null,
                transaction.getDescription(),
                excludeId);
    }

    /**
     * Clear all transactions (useful for testing)
     */
    public void deleteAll() {
        transactionMapper.deleteAll();
    }
}

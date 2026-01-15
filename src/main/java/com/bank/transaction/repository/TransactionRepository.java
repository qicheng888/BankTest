package com.bank.transaction.repository;

import com.bank.transaction.entity.Transaction;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Transaction Repository
 * 
 * In-memory storage implementation for transactions.
 * Uses ConcurrentHashMap for thread-safe operations.
 */
@Repository
public class TransactionRepository {

    /**
     * Primary storage: ID -> Transaction
     */
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    /**
     * Index for duplicate detection: hash -> transaction ID
     */
    private final Map<String, String> duplicateIndex = new ConcurrentHashMap<>();

    /**
     * Save a new transaction
     * 
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    public Transaction save(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
        // Update duplicate index
        String hash = transaction.generateDuplicateHash();
        duplicateIndex.put(hash, transaction.getId());
        return transaction;
    }

    /**
     * Find a transaction by ID
     * 
     * @param id the transaction ID
     * @return Optional containing the transaction if found
     */
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(transactions.get(id));
    }

    /**
     * Find all transactions
     * 
     * @return list of all transactions
     */
    public List<Transaction> findAll() {
        return new ArrayList<>(transactions.values());
    }

    /**
     * Find transactions with pagination
     * 
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated list of transactions
     */
    public List<Transaction> findAllPaginated(int page, int size) {
        return transactions.values().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Get total count of transactions
     * 
     * @return total number of transactions
     */
    public long count() {
        return transactions.size();
    }

    /**
     * Delete a transaction by ID
     * 
     * @param id the transaction ID
     * @return true if deleted, false if not found
     */
    public boolean deleteById(String id) {
        Transaction removed = transactions.remove(id);
        if (removed != null) {
            // Remove from duplicate index
            String hash = removed.generateDuplicateHash();
            duplicateIndex.remove(hash);
            return true;
        }
        return false;
    }

    /**
     * Check if a transaction exists by ID
     * 
     * @param id the transaction ID
     * @return true if exists
     */
    public boolean existsById(String id) {
        return transactions.containsKey(id);
    }

    /**
     * Check if a potential duplicate exists
     * 
     * @param transaction the transaction to check
     * @return true if a duplicate exists
     */
    public boolean existsDuplicate(Transaction transaction) {
        String hash = transaction.generateDuplicateHash();
        return duplicateIndex.containsKey(hash);
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
        String hash = transaction.generateDuplicateHash();
        String existingId = duplicateIndex.get(hash);
        return existingId != null && !existingId.equals(excludeId);
    }

    /**
     * Clear all transactions (useful for testing)
     */
    public void deleteAll() {
        transactions.clear();
        duplicateIndex.clear();
    }
}

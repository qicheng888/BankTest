package com.bank.transaction.service;

import com.bank.transaction.config.CacheConfig;
import com.bank.transaction.dto.PageResponse;
import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.exception.DuplicateTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Service Implementation
 * 
 * Business logic for transaction management with caching support.
 * Updated for JDK 21 with Record DTOs.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;

    @Value("${app.pagination.default-page-size:10}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Caching(put = @CachePut(value = CacheConfig.TRANSACTION_CACHE, key = "#result.id"), evict = @CacheEvict(value = CacheConfig.TRANSACTION_LIST_CACHE, allEntries = true))
    public TransactionResponse createTransaction(TransactionRequest request) {
        logger.debug("Creating new transaction: {}", request);

        // Build transaction entity from Record DTO
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .amount(request.amount())
                .type(request.type())
                .category(request.category())
                .description(request.description())
                .timestamp(LocalDateTime.now())
                .build();

        // Check for duplicates
        if (transactionRepository.existsDuplicate(transaction)) {
            throw new DuplicateTransactionException(
                    "Duplicate transaction detected: A transaction with the same amount, type, category and description already exists",
                    transaction.generateDuplicateHash());
        }

        // Save transaction
        Transaction saved = transactionRepository.save(transaction);
        logger.info("Created transaction with ID: {}", saved.getId());

        return TransactionResponse.fromEntity(saved);
    }

    @Override
    @Cacheable(value = CacheConfig.TRANSACTION_CACHE, key = "#id")
    public TransactionResponse getTransaction(String id) {
        logger.debug("Getting transaction by ID: {}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        return TransactionResponse.fromEntity(transaction);
    }

    @Override
    @Cacheable(value = CacheConfig.TRANSACTION_LIST_CACHE, key = "'page_' + #page + '_size_' + #size")
    public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {
        logger.debug("Getting all transactions - page: {}, size: {}", page, size);

        // Validate and adjust page size
        int adjustedSize = size <= 0 ? defaultPageSize : Math.min(size, maxPageSize);
        int adjustedPage = Math.max(page, 0);

        var transactions = transactionRepository.findAllPaginated(adjustedPage, adjustedSize);
        long total = transactionRepository.count();

        var content = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .toList(); // JDK 16+ Stream.toList()

        return PageResponse.of(content, adjustedPage, adjustedSize, total);
    }

    @Override
    @Caching(put = @CachePut(value = CacheConfig.TRANSACTION_CACHE, key = "#id"), evict = @CacheEvict(value = CacheConfig.TRANSACTION_LIST_CACHE, allEntries = true))
    public TransactionResponse updateTransaction(String id, TransactionRequest request) {
        logger.debug("Updating transaction ID: {} with data: {}", id, request);

        // Find existing transaction
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        // Build updated transaction from Record DTO
        Transaction updated = Transaction.builder()
                .id(id)
                .amount(request.amount())
                .type(request.type())
                .category(request.category())
                .description(request.description())
                .timestamp(existing.getTimestamp()) // Preserve original timestamp
                .build();

        // Check for duplicates (excluding self)
        if (transactionRepository.existsDuplicateExcluding(updated, id)) {
            throw new DuplicateTransactionException(
                    "Duplicate transaction detected: A transaction with the same amount, type, category and description already exists",
                    updated.generateDuplicateHash());
        }

        // Save updated transaction
        Transaction saved = transactionRepository.save(updated);
        logger.info("Updated transaction with ID: {}", saved.getId());

        return TransactionResponse.fromEntity(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.TRANSACTION_CACHE, key = "#id"),
            @CacheEvict(value = CacheConfig.TRANSACTION_LIST_CACHE, allEntries = true)
    })
    public void deleteTransaction(String id) {
        logger.debug("Deleting transaction with ID: {}", id);

        if (!transactionRepository.existsById(id)) {
            throw new TransactionNotFoundException(id);
        }

        boolean deleted = transactionRepository.deleteById(id);
        if (deleted) {
            logger.info("Deleted transaction with ID: {}", id);
        }
    }
}

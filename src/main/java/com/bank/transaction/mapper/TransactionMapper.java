package com.bank.transaction.mapper;

import com.bank.transaction.entity.Transaction;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper for Transaction entity.
 * Provides database operations for transaction management.
 */
@Mapper
public interface TransactionMapper {

    /**
     * Insert a new transaction
     */
    @Insert("""
            INSERT INTO transactions (id, amount, type, category, description, timestamp)
            VALUES (#{id}, #{amount}, #{type}, #{category}, #{description}, #{timestamp})
            """)
    int insert(Transaction transaction);

    /**
     * Find transaction by ID
     */
    @Select("SELECT * FROM transactions WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "type", column = "type"),
            @Result(property = "category", column = "category"),
            @Result(property = "description", column = "description"),
            @Result(property = "timestamp", column = "timestamp")
    })
    Optional<Transaction> findById(String id);

    /**
     * Find all transactions
     */
    @Select("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<Transaction> findAll();

    /**
     * Find transactions with pagination (offset-based)
     */
    @Select("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT #{limit} OFFSET #{offset}")
    List<Transaction> findAllPaginated(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * Count total transactions
     */
    @Select("SELECT COUNT(*) FROM transactions")
    long count();

    /**
     * Update an existing transaction
     */
    @Update("""
            UPDATE transactions
            SET amount = #{amount}, type = #{type}, category = #{category},
                description = #{description}, timestamp = #{timestamp}
            WHERE id = #{id}
            """)
    int update(Transaction transaction);

    /**
     * Delete transaction by ID
     */
    @Delete("DELETE FROM transactions WHERE id = #{id}")
    int deleteById(String id);

    /**
     * Check if transaction exists by ID
     */
    @Select("SELECT COUNT(*) > 0 FROM transactions WHERE id = #{id}")
    boolean existsById(String id);

    /**
     * Check if a duplicate transaction exists (by content hash)
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM transactions
            WHERE amount = #{amount} AND type = #{type} AND category = #{category}
            AND (description = #{description} OR (description IS NULL AND #{description} IS NULL))
            """)
    boolean existsDuplicate(@Param("amount") BigDecimal amount,
            @Param("type") String type,
            @Param("category") String category,
            @Param("description") String description);

    /**
     * Check if a duplicate exists excluding a specific ID (for updates)
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM transactions
            WHERE amount = #{amount} AND type = #{type} AND category = #{category}
            AND (description = #{description} OR (description IS NULL AND #{description} IS NULL))
            AND id != #{excludeId}
            """)
    boolean existsDuplicateExcluding(@Param("amount") BigDecimal amount,
            @Param("type") String type,
            @Param("category") String category,
            @Param("description") String description,
            @Param("excludeId") String excludeId);

    /**
     * Delete all transactions (for testing)
     */
    @Delete("DELETE FROM transactions")
    void deleteAll();
}

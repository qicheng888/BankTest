package com.bank.transaction.dto;

import com.bank.transaction.entity.Transaction;
import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Response DTO (Record)
 * 
 * JDK 21 Record class for returning transaction data to clients.
 * Records provide immutable data with auto-generated equals, hashCode, and
 * toString.
 */
public record TransactionResponse(String id,BigDecimal amount,TransactionType type,String typeDisplayName,TransactionCategory category,String categoryDisplayName,String description,LocalDateTime timestamp){
/**
 * Convert Transaction entity to Response DTO
 */
public static TransactionResponse fromEntity(Transaction transaction){if(transaction==null){return null;}return new TransactionResponse(transaction.getId(),transaction.getAmount(),transaction.getType(),transaction.getType()!=null?transaction.getType().getDisplayName():null,transaction.getCategory(),transaction.getCategory()!=null?transaction.getCategory().getDisplayName():null,transaction.getDescription(),transaction.getTimestamp());}}

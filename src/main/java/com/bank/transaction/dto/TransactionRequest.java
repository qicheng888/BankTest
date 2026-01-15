package com.bank.transaction.dto;

import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Transaction Request DTO (Record)
 * 
 * JDK 21 Record class for creating and updating transactions.
 * Contains validation annotations for input validation.
 * 
 * Note: Using jakarta.validation (Spring Boot 3.x / Jakarta EE)
 */
public record TransactionRequest(@NotNull(message="Transaction amount cannot be empty")@DecimalMin(value="0.01",message="Transaction amount must be greater than 0")BigDecimal amount,

@NotNull(message="Transaction type cannot be empty")TransactionType type,

@NotNull(message="Transaction category cannot be empty")TransactionCategory category,

@Size(max=500,message="Description cannot exceed 500 characters")String description){}

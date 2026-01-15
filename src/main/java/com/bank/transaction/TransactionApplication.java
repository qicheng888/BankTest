package com.bank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Transaction Management System Application
 * 
 * A Spring Boot application for managing bank transactions.
 * Features:
 * - RESTful API for CRUD operations
 * - In-memory data storage
 * - Caffeine caching
 * - Pagination support
 * - Web UI for transaction management
 */
@SpringBootApplication
@EnableCaching
public class TransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionApplication.class, args);
    }
}

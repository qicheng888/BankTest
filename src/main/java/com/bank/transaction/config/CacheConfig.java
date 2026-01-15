package com.bank.transaction.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration
 * 
 * Configures Caffeine cache for high-performance caching with
 * optimized settings for cache-data consistency.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TRANSACTION_CACHE = "transactions";
    public static final String TRANSACTION_LIST_CACHE = "transactionList";

    @Value("${app.cache.transaction.max-size:1000}")
    private int maxSize;

    @Value("${app.cache.transaction.expire-after-write-seconds:300}")
    private int expireAfterWriteSeconds;

    @Value("${app.cache.transaction-list.max-size:100}")
    private int listCacheMaxSize;

    @Value("${app.cache.transaction-list.expire-after-write-seconds:60}")
    private int listCacheExpireSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Register caches with different configurations
        cacheManager.registerCustomCache(TRANSACTION_CACHE,
                buildTransactionCache().build());
        cacheManager.registerCustomCache(TRANSACTION_LIST_CACHE,
                buildListCache().build());

        return cacheManager;
    }

    /**
     * Cache configuration for individual transactions.
     * Longer TTL since individual records change less frequently.
     */
    private Caffeine<Object, Object> buildTransactionCache() {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
                .recordStats();
    }

    /**
     * Cache configuration for transaction lists.
     * Shorter TTL to ensure consistency with database state.
     * Lists are invalidated on any write operation.
     */
    private Caffeine<Object, Object> buildListCache() {
        return Caffeine.newBuilder()
                .maximumSize(listCacheMaxSize)
                .expireAfterWrite(listCacheExpireSeconds, TimeUnit.SECONDS)
                .recordStats();
    }
}

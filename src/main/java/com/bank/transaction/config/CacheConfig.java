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
 * Configures Caffeine cache for high-performance caching.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TRANSACTION_CACHE = "transactions";
    public static final String TRANSACTION_LIST_CACHE = "transactionList";

    @Value("${app.cache.transaction.max-size:1000}")
    private int maxSize;

    @Value("${app.cache.transaction.expire-after-write-seconds:600}")
    private int expireAfterWriteSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                TRANSACTION_CACHE,
                TRANSACTION_LIST_CACHE);
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
                .recordStats();
    }
}

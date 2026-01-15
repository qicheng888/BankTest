package com.bank.transaction.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Virtual Thread Configuration
 * 
 * JDK 21 Virtual Threads (Project Loom) configuration for Tomcat.
 * Enables lightweight virtual threads for handling HTTP requests,
 * significantly improving throughput for I/O-bound workloads.
 */
@Configuration
public class VirtualThreadConfig {

    /**
     * Configure Tomcat to use Virtual Threads per task executor.
     * Each incoming request will be handled by a virtual thread.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutor() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}

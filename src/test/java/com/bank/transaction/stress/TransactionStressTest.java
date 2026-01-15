package com.bank.transaction.stress;

import com.bank.transaction.dto.PageResponse;
import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;
import com.bank.transaction.repository.TransactionRepository;
import com.bank.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transaction Stress Tests
 * 
 * Performance and concurrency tests for the transaction service.
 * Updated for JDK 21 with Record DTOs and Virtual Threads.
 */
@SpringBootTest
class TransactionStressTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("并发创建交易测试 - 100 并发 (Virtual Threads)")
    void concurrentCreateTransactions() throws InterruptedException {
        int threadCount = 100;
        // JDK 21: Using Virtual Threads for better scalability
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Using Record constructor
                    var request = new TransactionRequest(
                            new BigDecimal(100 + index),
                            TransactionType.values()[index % 3],
                            TransactionCategory.values()[index % 8],
                            "Concurrent transaction " + index);

                    TransactionResponse response = transactionService.createTransaction(request);
                    if (response != null && response.id() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("========== 并发创建测试结果 (Virtual Threads) ==========");
        System.out.println("线程数: " + threadCount);
        System.out.println("成功数: " + successCount.get());
        System.out.println("失败数: " + failCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每秒: " + (threadCount * 1000.0 / duration) + " 次");
        System.out.println("====================================");

        assertEquals(threadCount, successCount.get());
        assertEquals(0, failCount.get());
    }

    @Test
    @DisplayName("高负载读取测试 - 1000 次读取 (Virtual Threads)")
    void highLoadReadTransactions() throws InterruptedException {
        // Create some initial data
        List<String> transactionIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            var request = new TransactionRequest(
                    new BigDecimal(100 + i),
                    TransactionType.DEPOSIT,
                    TransactionCategory.SALARY,
                    "Data for read test " + i);
            TransactionResponse response = transactionService.createTransaction(request);
            transactionIds.add(response.id());
        }

        int readCount = 1000;
        // JDK 21: Using Virtual Threads
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(readCount);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < readCount; i++) {
            final String id = transactionIds.get(i % transactionIds.size());
            executor.submit(() -> {
                try {
                    TransactionResponse response = transactionService.getTransaction(id);
                    if (response != null) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("========== 高负载读取测试结果 (Virtual Threads) ==========");
        System.out.println("读取次数: " + readCount);
        System.out.println("成功数: " + successCount.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每秒: " + (readCount * 1000.0 / duration) + " 次");
        System.out.println("平均延迟: " + (duration * 1.0 / readCount) + "ms");
        System.out.println("========================================");

        assertEquals(readCount, successCount.get());
    }

    @Test
    @DisplayName("分页查询性能测试")
    void paginationPerformanceTest() {
        // Create 1000 transactions
        int totalRecords = 1000;
        for (int i = 0; i < totalRecords; i++) {
            var request = new TransactionRequest(
                    new BigDecimal(100 + i),
                    TransactionType.values()[i % 3],
                    TransactionCategory.values()[i % 8],
                    "Pagination test " + i);
            transactionService.createTransaction(request);
        }

        // Test pagination performance
        int pageSize = 20;
        int totalPages = (totalRecords + pageSize - 1) / pageSize;

        long startTime = System.currentTimeMillis();

        for (int page = 0; page < totalPages; page++) {
            PageResponse<TransactionResponse> response = transactionService.getAllTransactions(page, pageSize);
            assertNotNull(response);
            // Each page should have content (last page may have less items)
            assertTrue(response.content().size() > 0 || page == totalPages);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("========== 分页查询性能测试结果 ==========");
        System.out.println("总记录数: " + totalRecords);
        System.out.println("页大小: " + pageSize);
        System.out.println("总页数: " + totalPages);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每页: " + (duration * 1.0 / totalPages) + "ms");
        System.out.println("==========================================");

        assertTrue(duration < 5000, "分页查询应在5秒内完成");
    }

    @Test
    @DisplayName("混合操作压力测试 - CRUD并发 (Virtual Threads)")
    void mixedOperationsStressTest() throws InterruptedException {
        // Prepare some initial data
        List<String> transactionIds = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 20; i++) {
            var request = new TransactionRequest(
                    new BigDecimal(1000 + i),
                    TransactionType.DEPOSIT,
                    TransactionCategory.SALARY,
                    "Initial data " + i);
            TransactionResponse response = transactionService.createTransaction(request);
            transactionIds.add(response.id());
        }

        int operationsPerType = 50;
        int totalOperations = operationsPerType * 4; // Create, Read, Update, Delete
        // JDK 21: Using Virtual Threads
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(totalOperations);

        AtomicInteger createSuccess = new AtomicInteger(0);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger updateSuccess = new AtomicInteger(0);
        AtomicInteger deleteSuccess = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Create operations
        for (int i = 0; i < operationsPerType; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    var request = new TransactionRequest(
                            new BigDecimal(2000 + index),
                            TransactionType.TRANSFER,
                            TransactionCategory.OTHER,
                            "Mixed test create " + index);
                    TransactionResponse response = transactionService.createTransaction(request);
                    if (response != null) {
                        transactionIds.add(response.id());
                        createSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        // Read operations
        for (int i = 0; i < operationsPerType; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (!transactionIds.isEmpty()) {
                        String id = transactionIds.get(index % transactionIds.size());
                        TransactionResponse response = transactionService.getTransaction(id);
                        if (response != null) {
                            readSuccess.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Ignore - may be deleted
                } finally {
                    latch.countDown();
                }
            });
        }

        // Update operations
        for (int i = 0; i < operationsPerType; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (!transactionIds.isEmpty()) {
                        String id = transactionIds.get(index % Math.min(transactionIds.size(), 20));
                        var request = new TransactionRequest(
                                new BigDecimal(3000 + index),
                                TransactionType.WITHDRAWAL,
                                TransactionCategory.SHOPPING,
                                "Updated " + index);
                        TransactionResponse response = transactionService.updateTransaction(id, request);
                        if (response != null) {
                            updateSuccess.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        // Delete operations
        for (int i = 0; i < operationsPerType; i++) {
            executor.submit(() -> {
                try {
                    if (transactionIds.size() > 30) {
                        String id = transactionIds.remove(transactionIds.size() - 1);
                        transactionService.deleteTransaction(id);
                        deleteSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("========== 混合操作压力测试结果 (Virtual Threads) ==========");
        System.out.println("总操作数: " + totalOperations);
        System.out.println("创建成功: " + createSuccess.get());
        System.out.println("读取成功: " + readSuccess.get());
        System.out.println("更新成功: " + updateSuccess.get());
        System.out.println("删除成功: " + deleteSuccess.get());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每秒: " + (totalOperations * 1000.0 / duration) + " 次");
        System.out.println("==========================================");

        // At least some operations should succeed
        assertTrue(createSuccess.get() > 0);
        assertTrue(readSuccess.get() > 0);
    }

    @Test
    @DisplayName("大数据量写入性能测试 - 1000 条")
    void bulkInsertPerformanceTest() {
        int totalRecords = 1000;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRecords; i++) {
            var request = new TransactionRequest(
                    new BigDecimal(100 + i),
                    TransactionType.values()[i % 3],
                    TransactionCategory.values()[i % 8],
                    "Bulk insert test " + i);
            transactionService.createTransaction(request);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("========== 大数据量写入测试结果 ==========");
        System.out.println("总记录数: " + totalRecords);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均每条: " + (duration * 1.0 / totalRecords) + "ms");
        System.out.println("每秒写入: " + (totalRecords * 1000.0 / duration) + " 条");
        System.out.println("==========================================");

        assertEquals(totalRecords, transactionRepository.count());
        assertTrue(duration < 30000, "1000条记录写入应在30秒内完成");
    }
}

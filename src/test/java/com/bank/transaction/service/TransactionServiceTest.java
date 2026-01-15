package com.bank.transaction.service;

import com.bank.transaction.dto.PageResponse;
import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;
import com.bank.transaction.exception.DuplicateTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transaction Service Unit Tests
 * 
 * Tests the business logic in the transaction service layer.
 * Updated for JDK 21 with Record DTOs.
 */
@SpringBootTest
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("创建交易 - 成功")
    void createTransaction_Success() {
        // Using Record constructor
        var request = new TransactionRequest(
                new BigDecimal("1000.00"),
                TransactionType.DEPOSIT,
                TransactionCategory.SALARY,
                "月度工资");

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(new BigDecimal("1000.00"), response.amount());
        assertEquals(TransactionType.DEPOSIT, response.type());
        assertEquals(TransactionCategory.SALARY, response.category());
        assertEquals("月度工资", response.description());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("创建交易 - 无描述成功")
    void createTransaction_NoDescription_Success() {
        var request = new TransactionRequest(
                new BigDecimal("50.00"),
                TransactionType.WITHDRAWAL,
                TransactionCategory.FOOD,
                null);

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertNull(response.description());
    }

    @Test
    @DisplayName("创建重复交易 - 抛出异常")
    void createTransaction_Duplicate_ThrowsException() {
        var request = new TransactionRequest(
                new BigDecimal("200.00"),
                TransactionType.TRANSFER,
                TransactionCategory.OTHER,
                "重复测试");

        // First creation should succeed
        transactionService.createTransaction(request);

        // Second creation should throw exception
        DuplicateTransactionException exception = assertThrows(
                DuplicateTransactionException.class,
                () -> transactionService.createTransaction(request));

        assertTrue(exception.getMessage().contains("Duplicate transaction"));
    }

    @Test
    @DisplayName("获取交易 - 成功")
    void getTransaction_Success() {
        var request = new TransactionRequest(
                new BigDecimal("300.00"),
                TransactionType.DEPOSIT,
                TransactionCategory.SALARY,
                null);

        TransactionResponse created = transactionService.createTransaction(request);
        TransactionResponse retrieved = transactionService.getTransaction(created.id());

        assertEquals(created.id(), retrieved.id());
        assertEquals(created.amount(), retrieved.amount());
    }

    @Test
    @DisplayName("获取交易 - 不存在抛出异常")
    void getTransaction_NotFound_ThrowsException() {
        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.getTransaction("non-existent-id"));
    }

    @Test
    @DisplayName("分页获取交易 - 成功")
    void getAllTransactions_Paginated() {
        // Create 25 transactions
        for (int i = 0; i < 25; i++) {
            var request = new TransactionRequest(
                    new BigDecimal(100 + i),
                    TransactionType.DEPOSIT,
                    TransactionCategory.SALARY,
                    "Transaction " + i);
            transactionService.createTransaction(request);
        }

        // Test first page
        PageResponse<TransactionResponse> page1 = transactionService.getAllTransactions(0, 10);
        assertEquals(10, page1.content().size());
        assertEquals(0, page1.page());
        assertEquals(25, page1.totalElements());
        assertEquals(3, page1.totalPages());
        assertTrue(page1.first());
        assertFalse(page1.last());

        // Test last page
        PageResponse<TransactionResponse> page3 = transactionService.getAllTransactions(2, 10);
        assertEquals(5, page3.content().size());
        assertFalse(page3.first());
        assertTrue(page3.last());
    }

    @Test
    @DisplayName("分页获取 - 空结果")
    void getAllTransactions_Empty() {
        PageResponse<TransactionResponse> page = transactionService.getAllTransactions(0, 10);

        assertNotNull(page);
        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
    }

    @Test
    @DisplayName("分页获取 - 页码超出范围")
    void getAllTransactions_PageOutOfRange() {
        var request = new TransactionRequest(
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT,
                TransactionCategory.OTHER,
                null);
        transactionService.createTransaction(request);

        PageResponse<TransactionResponse> page = transactionService.getAllTransactions(10, 10);

        assertTrue(page.content().isEmpty());
        assertEquals(1, page.totalElements());
    }

    @Test
    @DisplayName("更新交易 - 成功")
    void updateTransaction_Success() {
        // Create transaction
        var createRequest = new TransactionRequest(
                new BigDecimal("500.00"),
                TransactionType.WITHDRAWAL,
                TransactionCategory.SHOPPING,
                "原始描述");

        TransactionResponse created = transactionService.createTransaction(createRequest);

        // Update transaction
        var updateRequest = new TransactionRequest(
                new BigDecimal("600.00"),
                TransactionType.WITHDRAWAL,
                TransactionCategory.ENTERTAINMENT,
                "更新后描述");

        TransactionResponse updated = transactionService.updateTransaction(created.id(), updateRequest);

        assertEquals(created.id(), updated.id());
        assertEquals(new BigDecimal("600.00"), updated.amount());
        assertEquals(TransactionCategory.ENTERTAINMENT, updated.category());
        assertEquals("更新后描述", updated.description());
        // Timestamp should be preserved
        assertEquals(created.timestamp(), updated.timestamp());
    }

    @Test
    @DisplayName("更新交易 - 不存在抛出异常")
    void updateTransaction_NotFound_ThrowsException() {
        var request = new TransactionRequest(
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT,
                TransactionCategory.OTHER,
                null);

        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.updateTransaction("non-existent-id", request));
    }

    @Test
    @DisplayName("删除交易 - 成功")
    void deleteTransaction_Success() {
        var request = new TransactionRequest(
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT,
                TransactionCategory.OTHER,
                null);

        TransactionResponse created = transactionService.createTransaction(request);
        String id = created.id();

        // Delete should not throw
        assertDoesNotThrow(() -> transactionService.deleteTransaction(id));

        // Subsequent get should throw not found
        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.getTransaction(id));
    }

    @Test
    @DisplayName("删除交易 - 不存在抛出异常")
    void deleteTransaction_NotFound_ThrowsException() {
        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.deleteTransaction("non-existent-id"));
    }

    @Test
    @DisplayName("删除后可以创建相同内容的交易")
    void createAfterDelete_SameContent_Success() {
        var request = new TransactionRequest(
                new BigDecimal("999.00"),
                TransactionType.TRANSFER,
                TransactionCategory.OTHER,
                "删除后重建测试");

        // Create and delete
        TransactionResponse created = transactionService.createTransaction(request);
        transactionService.deleteTransaction(created.id());

        // Should be able to create again
        TransactionResponse recreated = transactionService.createTransaction(request);
        assertNotNull(recreated);
        assertNotEquals(created.id(), recreated.id());
    }
}

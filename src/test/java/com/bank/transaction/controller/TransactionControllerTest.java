package com.bank.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;
import com.bank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Transaction Controller Integration Tests
 * 
 * Tests the REST API endpoints for transaction management.
 * Updated for JDK 21 with Record DTOs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TransactionRepository transactionRepository;

        @BeforeEach
        void setUp() {
                transactionRepository.deleteAll();
        }

        @Test
        @DisplayName("创建交易 - 成功")
        void createTransaction_Success() throws Exception {
                // Using Record constructor
                var request = new TransactionRequest(
                                new BigDecimal("1000.00"),
                                TransactionType.DEPOSIT,
                                TransactionCategory.SALARY,
                                "工资收入");

                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.amount").value(1000.00))
                                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                                .andExpect(jsonPath("$.category").value("SALARY"))
                                .andExpect(jsonPath("$.description").value("工资收入"))
                                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("创建交易 - 金额为空失败")
        void createTransaction_AmountNull_Fail() throws Exception {
                var request = new TransactionRequest(
                                null,
                                TransactionType.DEPOSIT,
                                TransactionCategory.SALARY,
                                null);

                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.validationErrors.amount").isNotEmpty());
        }

        @Test
        @DisplayName("创建交易 - 金额为负数失败")
        void createTransaction_NegativeAmount_Fail() throws Exception {
                var request = new TransactionRequest(
                                new BigDecimal("-100.00"),
                                TransactionType.WITHDRAWAL,
                                TransactionCategory.SHOPPING,
                                null);

                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("创建重复交易 - 失败")
        void createTransaction_Duplicate_Fail() throws Exception {
                var request = new TransactionRequest(
                                new BigDecimal("500.00"),
                                TransactionType.WITHDRAWAL,
                                TransactionCategory.SHOPPING,
                                "购物消费");

                // Create first transaction
                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // Attempt to create duplicate
                mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error").value("Conflict"))
                                .andExpect(jsonPath("$.message").value(containsString("Duplicate transaction")));
        }

        @Test
        @DisplayName("获取交易 - 成功")
        void getTransaction_Success() throws Exception {
                // Create a transaction first
                var request = new TransactionRequest(
                                new BigDecimal("200.00"),
                                TransactionType.TRANSFER,
                                TransactionCategory.OTHER,
                                "转账测试");

                MvcResult createResult = mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("id").asText();

                // Get the transaction
                mockMvc.perform(get("/api/transactions/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id))
                                .andExpect(jsonPath("$.amount").value(200.00));
        }

        @Test
        @DisplayName("获取不存在的交易 - 失败")
        void getTransaction_NotFound_Fail() throws Exception {
                mockMvc.perform(get("/api/transactions/{id}", "non-existent-id"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        @DisplayName("获取所有交易 - 分页")
        void getAllTransactions_Paginated() throws Exception {
                // Create multiple transactions
                for (int i = 0; i < 15; i++) {
                        var request = new TransactionRequest(
                                        new BigDecimal(100 + i),
                                        TransactionType.DEPOSIT,
                                        TransactionCategory.SALARY,
                                        "交易 " + i);

                        mockMvc.perform(post("/api/transactions")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated());
                }

                // Get first page
                mockMvc.perform(get("/api/transactions")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(10)))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(15))
                                .andExpect(jsonPath("$.totalPages").value(2))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(false));

                // Get second page
                mockMvc.perform(get("/api/transactions")
                                .param("page", "1")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(5)))
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.first").value(false))
                                .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("更新交易 - 成功")
        void updateTransaction_Success() throws Exception {
                // Create a transaction first
                var createRequest = new TransactionRequest(
                                new BigDecimal("300.00"),
                                TransactionType.WITHDRAWAL,
                                TransactionCategory.FOOD,
                                "餐饮消费");

                MvcResult createResult = mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("id").asText();

                // Update the transaction
                var updateRequest = new TransactionRequest(
                                new BigDecimal("350.00"),
                                TransactionType.WITHDRAWAL,
                                TransactionCategory.ENTERTAINMENT,
                                "娱乐消费 - 已更新");

                mockMvc.perform(put("/api/transactions/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id))
                                .andExpect(jsonPath("$.amount").value(350.00))
                                .andExpect(jsonPath("$.category").value("ENTERTAINMENT"))
                                .andExpect(jsonPath("$.description").value("娱乐消费 - 已更新"));
        }

        @Test
        @DisplayName("更新不存在的交易 - 失败")
        void updateTransaction_NotFound_Fail() throws Exception {
                var request = new TransactionRequest(
                                new BigDecimal("100.00"),
                                TransactionType.DEPOSIT,
                                TransactionCategory.OTHER,
                                null);

                mockMvc.perform(put("/api/transactions/{id}", "non-existent-id")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("删除交易 - 成功")
        void deleteTransaction_Success() throws Exception {
                // Create a transaction first
                var request = new TransactionRequest(
                                new BigDecimal("150.00"),
                                TransactionType.DEPOSIT,
                                TransactionCategory.OTHER,
                                "测试删除");

                MvcResult createResult = mockMvc.perform(post("/api/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("id").asText();

                // Delete the transaction
                mockMvc.perform(delete("/api/transactions/{id}", id))
                                .andExpect(status().isNoContent());

                // Verify it's deleted
                mockMvc.perform(get("/api/transactions/{id}", id))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("删除不存在的交易 - 失败")
        void deleteTransaction_NotFound_Fail() throws Exception {
                mockMvc.perform(delete("/api/transactions/{id}", "non-existent-id"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Not Found"));
        }
}

package com.bank.transaction.controller;

import com.bank.transaction.enums.TransactionCategory;
import com.bank.transaction.enums.TransactionType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Page Controller for Thymeleaf Views
 * 
 * Serves the main transaction management page which communicates
 * with the REST API for CRUD operations.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index(Model model) {
        // Pass enum values to template for dropdown options
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("transactionCategories", TransactionCategory.values());
        return "index";
    }
}

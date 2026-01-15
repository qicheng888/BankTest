package com.bank.transaction.enums;

/**
 * Transaction Category Enumeration
 * 
 * Represents the category/purpose of a transaction:
 * - SALARY: Income from employment
 * - SHOPPING: Retail purchases
 * - FOOD: Food and dining expenses
 * - ENTERTAINMENT: Entertainment expenses
 * - UTILITIES: Utility bills
 * - HEALTHCARE: Medical expenses
 * - TRANSPORTATION: Travel and transport
 * - OTHER: Uncategorized transactions
 */
public enum TransactionCategory {
    SALARY("Salary"),
    SHOPPING("Shopping"),
    FOOD("Food & Dining"),
    ENTERTAINMENT("Entertainment"),
    UTILITIES("Utilities"),
    HEALTHCARE("Healthcare"),
    TRANSPORTATION("Transportation"),
    OTHER("Other");

    private final String displayName;

    TransactionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

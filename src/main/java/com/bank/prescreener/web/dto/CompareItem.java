package com.bank.prescreener.web.dto;

/** One product's result when comparing the same applicant across products. */
public record CompareItem(
        String product,
        double annualRate,
        double minAmount,
        double maxAmount,
        boolean withinRange,
        double newEmi,
        double foir,
        String verdict,
        String reasonCode) {
}

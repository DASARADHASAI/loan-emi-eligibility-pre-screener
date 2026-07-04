package com.bank.prescreener.web.dto;

/** A configurable interest product (from the central tariff config). */
public record RateSlab(String product, double minAmount, double maxAmount, double annualRate) {
}

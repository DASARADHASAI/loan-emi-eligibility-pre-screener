package com.bank.prescreener.web.dto;

/**
 * Incoming screening request. Wrapper types so a missing/blank field arrives as
 * {@code null} and is turned into a clean validation error rather than a 400/NPE.
 */
public record ScreenRequest(
        Double income,
        Double existingEmi,
        Double loanAmount,
        Double annualRatePct,
        Integer tenureMonths) {
}

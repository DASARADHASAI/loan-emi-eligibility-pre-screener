package com.bank.prescreener.web.dto;

/** Compare an applicant against every product — rate comes from each product, not here. */
public record CompareRequest(
        Double income,
        Double existingEmi,
        Double loanAmount,
        Integer tenureMonths) {
}

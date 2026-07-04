package com.bank.prescreener.foir;

/**
 * Applicant financials entered by the agent.
 *
 * @param income        monthly gross income
 * @param existingEmi   sum of existing monthly obligations
 * @param loanAmount    principal of the requested loan
 * @param annualRatePct annual interest rate, as a percent (e.g. 10.5)
 * @param tenureMonths  loan tenure in whole months
 */
public record ScreenInputs(
        double income,
        double existingEmi,
        double loanAmount,
        double annualRatePct,
        int tenureMonths) {
}

package com.bank.prescreener.foir;

/**
 * A successful pre-screen.
 *
 * @param newEmi           proposed EMI for the new loan, rounded per policy
 * @param totalObligations existingEmi + newEmi, rounded per policy
 * @param foir             FOIR percent rounded to 1 dp (drives verdict &amp; display)
 * @param foirExact        full-precision FOIR percent, kept for audit
 * @param verdict          ELIGIBLE / BORDERLINE / NOT_ELIGIBLE
 * @param reasonCode       machine-readable code, e.g. "ELIG_FOIR_OK"
 * @param reasonText       one line the agent reads to the customer
 */
public record ScreenResult(
        double newEmi,
        double totalObligations,
        double foir,
        double foirExact,
        Verdict verdict,
        String reasonCode,
        String reasonText) {
}

package com.bank.prescreener.foir;

/**
 * A counter-offer: instead of a dead-end "no", what the applicant CAN get.
 *
 * @param feasible        whether any positive loan qualifies at all (false when the
 *                        applicant's existing EMIs already exceed the FOIR limit)
 * @param maxEligibleLoan largest principal that keeps FOIR within the eligible band,
 *                        at the current rate and tenure (rounded down to a clean figure)
 * @param maxEligibleEmi  the EMI at that principal
 * @param tenureToQualify smallest whole-month tenure that would make the REQUESTED loan
 *                        amount eligible; -1 when tenure alone can't get there
 */
public record Advice(
        boolean feasible,
        double maxEligibleLoan,
        double maxEligibleEmi,
        int tenureToQualify) {
}

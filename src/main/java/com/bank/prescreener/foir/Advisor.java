package com.bank.prescreener.foir;

/**
 * Turns a rejection into a counter-offer. Pure maths, no I/O — fully unit-tested.
 *
 * <p>The core move is inverting the reducing-balance EMI formula: given the largest
 * EMI the applicant can afford under the FOIR rule, what principal (or what tenure)
 * gets them there?
 */
public final class Advisor {

    /** Beyond this we treat "extend the tenure" as impractical. */
    private static final int MAX_TENURE_MONTHS = 360;

    private Advisor() {
    }

    /**
     * Inverse of {@link FoirEngine#computeEmi}: the principal whose EMI is {@code emi}
     * at the given rate and tenure.
     */
    public static double principalForEmi(double emi, double annualRatePct, int tenureMonths) {
        double r = annualRatePct / 12.0 / 100.0;
        if (r == 0.0) {
            return emi * tenureMonths;
        }
        double growth = Math.pow(1.0 + r, tenureMonths);
        return emi * (growth - 1.0) / (r * growth);
    }

    /** Build the counter-offer for these inputs under this policy. */
    public static Advice advise(ScreenInputs in, PolicyConfig policy) {
        // The most the applicant can owe per month and still be eligible.
        double maxTotalObligation = policy.eligibleMax() / 100.0 * in.income();
        double maxNewEmi = maxTotalObligation - in.existingEmi();

        if (maxNewEmi <= 0) {
            // Existing obligations already reach the limit — no new loan fits.
            return new Advice(false, 0.0, 0.0, -1);
        }

        double maxLoanRaw = principalForEmi(maxNewEmi, in.annualRatePct(), in.tenureMonths());
        // Round DOWN to a clean ₹1,000 so the offer stays safely within the limit.
        double maxLoan = Math.floor(maxLoanRaw / 1000.0) * 1000.0;
        double maxLoanEmi = Money.round2(
                FoirEngine.computeEmi(maxLoan, in.annualRatePct(), in.tenureMonths()),
                policy.roundingMode());

        int tenure = minTenureToQualify(in, maxNewEmi);
        return new Advice(true, maxLoan, maxLoanEmi, tenure);
    }

    /**
     * Smallest whole-month tenure at which the REQUESTED loan's EMI drops to
     * {@code maxNewEmi} or below. Returns -1 when even a very long tenure can't
     * (the monthly interest alone already exceeds what they can afford).
     */
    private static int minTenureToQualify(ScreenInputs in, double maxNewEmi) {
        double p = in.loanAmount();
        double r = in.annualRatePct() / 12.0 / 100.0;

        if (r == 0.0) {
            return clamp((int) Math.ceil(p / maxNewEmi));
        }

        double interestOnly = p * r; // the EMI floor as tenure → ∞
        if (maxNewEmi <= interestOnly) {
            return -1;
        }
        double growth = maxNewEmi / (maxNewEmi - interestOnly); // (1+r)^n
        double n = Math.log(growth) / Math.log(1.0 + r);
        return clamp((int) Math.ceil(n));
    }

    private static int clamp(int months) {
        if (months < 1) {
            return 1;
        }
        return months > MAX_TENURE_MONTHS ? -1 : months;
    }
}

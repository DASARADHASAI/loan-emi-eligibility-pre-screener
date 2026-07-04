package com.bank.prescreener.foir;

/**
 * The pure FOIR pre-screening engine — no I/O, so it is exhaustively unit-testable.
 *
 * <p>FOIR = Fixed-Obligation-to-Income-Ratio = (existing EMIs + proposed EMI) / income.
 *
 * <p>Concepts exercised (per the brief): primitive {@code double}/{@code int} types,
 * explicit casting, arithmetic operators, an if-else-if ladder and a {@code switch}
 * on the computed band, and input guards before any arithmetic.
 */
public final class FoirEngine {

    private FoirEngine() {
    }

    /**
     * Proposed EMI via the reducing-balance formula:
     * <pre>EMI = P·r·(1+r)^n / ((1+r)^n − 1),  r = monthly rate, n = tenure months.</pre>
     * When the rate is zero the formula would divide by zero, so we fall back to P/n.
     *
     * <p>WATCH-OUT: everything is computed in {@code double}. {@code principal} is a
     * double, so {@code principal / tenureMonths} is floating-point division — no
     * integer truncation of the ratio.
     */
    public static double computeEmi(double principal, double annualRatePct, int tenureMonths) {
        double monthlyRate = annualRatePct / 12.0 / 100.0;
        if (monthlyRate == 0.0) {
            return principal / tenureMonths;
        }
        double growth = Math.pow(1.0 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * growth) / (growth - 1.0);
    }

    /**
     * Guard every input before any arithmetic. Returns {@code null} when all inputs
     * are valid, otherwise a {@link ScreenError} — never throws, never returns NaN.
     */
    public static ScreenError validate(ScreenInputs in) {
        if (!Double.isFinite(in.income()) || in.income() <= 0) {
            return new ScreenError("ERR_INCOME_NONPOSITIVE",
                    "Monthly income must be greater than zero.");
        }
        if (!Double.isFinite(in.existingEmi()) || in.existingEmi() < 0) {
            return new ScreenError("ERR_EMI_NEGATIVE",
                    "Existing EMI cannot be negative.");
        }
        if (!Double.isFinite(in.loanAmount()) || in.loanAmount() <= 0) {
            return new ScreenError("ERR_LOAN_NONPOSITIVE",
                    "Loan amount must be greater than zero.");
        }
        if (!Double.isFinite(in.annualRatePct()) || in.annualRatePct() < 0) {
            return new ScreenError("ERR_RATE_NEGATIVE",
                    "Interest rate cannot be negative.");
        }
        if (in.tenureMonths() <= 0) {
            return new ScreenError("ERR_TENURE_NONPOSITIVE",
                    "Tenure must be a positive whole number of months.");
        }
        return null;
    }

    /** Full pre-screen: validate → compute EMI → compute FOIR → classify. */
    public static ScreenOutcome screen(ScreenInputs in, PolicyConfig policy) {
        ScreenError error = validate(in);
        if (error != null) {
            return ScreenOutcome.failure(error);
        }

        double rawEmi = computeEmi(in.loanAmount(), in.annualRatePct(), in.tenureMonths());
        double newEmi = Money.round2(rawEmi, policy.roundingMode());
        double totalObligations = Money.round2(in.existingEmi() + newEmi, policy.roundingMode());

        // Full-precision ratio; only rounded for display so the ratio is never truncated.
        double foirExact = (totalObligations / in.income()) * 100.0;
        double foir = Math.round(foirExact * 10.0) / 10.0; // 1 decimal place

        // Verdict via an explicit if-else-if ladder on the computed FOIR band.
        Verdict verdict;
        String reasonCode;
        if (foir <= policy.eligibleMax()) {
            verdict = Verdict.ELIGIBLE;
            reasonCode = "ELIG_FOIR_OK";
        } else if (foir <= policy.borderlineMax()) {
            verdict = Verdict.BORDERLINE;
            reasonCode = "BORDERLINE_REVIEW";
        } else {
            verdict = Verdict.NOT_ELIGIBLE;
            reasonCode = "REJECT_FOIR_HIGH";
        }

        // One-line reason the agent reads out — switch on the verdict band.
        String shown = String.format("%.1f", foir);
        String reasonText = switch (verdict) {
            case ELIGIBLE -> "FOIR " + shown + "% is within the " + fmt(policy.eligibleMax())
                    + "% limit — applicant is pre-eligible.";
            case BORDERLINE -> "FOIR " + shown + "% falls in the " + fmt(policy.eligibleMax())
                    + "–" + fmt(policy.borderlineMax()) + "% review band — refer for manual review.";
            case NOT_ELIGIBLE -> "FOIR " + shown + "% exceeds the " + fmt(policy.borderlineMax())
                    + "% ceiling — not pre-eligible.";
        };

        return ScreenOutcome.ok(new ScreenResult(
                newEmi, totalObligations, foir, foirExact, verdict, reasonCode, reasonText));
    }

    /** Format a threshold: 50.0 → "50", 52.5 → "52.5". Shows an explicit long cast. */
    private static String fmt(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}

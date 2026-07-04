package com.bank.prescreener.foir;

import java.math.RoundingMode;

/**
 * FOIR thresholds and rounding mode that drive the verdict. Sourced from the
 * database at runtime so tariffs are never hard-coded into the engine.
 *
 * @param eligibleMax   FOIR at or below this percent is ELIGIBLE
 * @param borderlineMax FOIR above eligibleMax and at/below this percent is BORDERLINE
 * @param roundingMode  rounding applied to EMI/obligation amounts
 */
public record PolicyConfig(double eligibleMax, double borderlineMax, RoundingMode roundingMode) {

    /** RBI-aligned defaults, used when the database is unreachable. */
    public static final PolicyConfig DEFAULT =
            new PolicyConfig(50.0, 55.0, RoundingMode.HALF_EVEN);
}

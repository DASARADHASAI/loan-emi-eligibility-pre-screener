package com.bank.prescreener.foir;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Currency rounding.
 *
 * WATCH-OUT addressed: "Banker's rounding vs simple rounding causes mismatches with
 * downstream systems." All money rounding funnels through here. Pass
 * {@link RoundingMode#HALF_EVEN} for banker's rounding (what core-banking ledgers
 * use) or {@link RoundingMode#HALF_UP} for simple rounding.
 *
 * We route the {@code double} through {@link BigDecimal#valueOf(double)}, which uses
 * the canonical decimal string of the value, so IEEE-754 artifacts don't corrupt the
 * half-way decision.
 */
public final class Money {

    private Money() {
    }

    /** Round to 2 decimal places (paise/cents) using the given mode. */
    public static double round2(double value, RoundingMode mode) {
        if (!Double.isFinite(value)) {
            return value;
        }
        return BigDecimal.valueOf(value).setScale(2, mode).doubleValue();
    }
}

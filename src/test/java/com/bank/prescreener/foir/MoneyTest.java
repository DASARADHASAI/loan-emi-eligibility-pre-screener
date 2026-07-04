package com.bank.prescreener.foir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money rounding — banker's vs simple")
class MoneyTest {

    @Test
    @DisplayName("banker's rounding rounds half to even")
    void bankersRoundsHalfToEven() {
        assertEquals(0.12, Money.round2(0.125, RoundingMode.HALF_EVEN)); // 12 even → stays
        assertEquals(0.14, Money.round2(0.135, RoundingMode.HALF_EVEN)); // 13 odd → up
        assertEquals(0.14, Money.round2(0.145, RoundingMode.HALF_EVEN)); // 14 even → stays
    }

    @Test
    @DisplayName("simple rounding rounds half up, diverging from banker's")
    void halfUpDivergesFromBankers() {
        assertEquals(0.13, Money.round2(0.125, RoundingMode.HALF_UP));
        assertEquals(0.15, Money.round2(0.145, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("handles negatives and non-finite values")
    void handlesEdgeValues() {
        assertEquals(-0.13, Money.round2(-0.125, RoundingMode.HALF_UP));
        assertEquals(Double.NaN, Money.round2(Double.NaN, RoundingMode.HALF_EVEN));
    }
}

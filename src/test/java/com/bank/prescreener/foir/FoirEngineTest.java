package com.bank.prescreener.foir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FOIR engine")
class FoirEngineTest {

    private static ScreenInputs inputs(double income, double existingEmi, double loan,
                                       double rate, int tenure) {
        return new ScreenInputs(income, existingEmi, loan, rate, tenure);
    }

    @Nested
    @DisplayName("computeEmi (reducing-balance)")
    class ComputeEmi {

        @Test
        @DisplayName("matches the known EMI for 5,00,000 @ 10% over 60 months")
        void knownEmi() {
            assertEquals(10623.522355, FoirEngine.computeEmi(500000, 10, 60), 1e-4);
        }

        @Test
        @DisplayName("uses P/n when the rate is zero (no divide-by-zero)")
        void zeroRate() {
            assertEquals(10000.0, FoirEngine.computeEmi(120000, 0, 12));
        }

        @Test
        @DisplayName("matches the known EMI for 10,00,000 @ 8.5% over 120 months")
        void knownEmi2() {
            assertEquals(12398.568887, FoirEngine.computeEmi(1000000, 8.5, 120), 1e-4);
        }
    }

    @Nested
    @DisplayName("verdict bands")
    class Bands {

        @Test
        @DisplayName("ELIGIBLE at exactly the 50% boundary")
        void eligibleBoundary() {
            ScreenOutcome out = FoirEngine.screen(inputs(100000, 0, 50000, 0, 1),
                    PolicyConfig.DEFAULT);
            assertTrue(out.ok());
            assertEquals(50.0, out.result().foir());
            assertEquals(Verdict.ELIGIBLE, out.result().verdict());
            assertEquals("ELIG_FOIR_OK", out.result().reasonCode());
        }

        @Test
        @DisplayName("BORDERLINE just above 50%")
        void borderline() {
            ScreenOutcome out = FoirEngine.screen(inputs(100000, 52000, 1, 0, 1),
                    PolicyConfig.DEFAULT);
            assertEquals(Verdict.BORDERLINE, out.result().verdict());
            assertEquals("BORDERLINE_REVIEW", out.result().reasonCode());
        }

        @Test
        @DisplayName("BORDERLINE at exactly the 55% boundary")
        void borderlineBoundary() {
            ScreenOutcome out = FoirEngine.screen(inputs(100000, 55000, 1, 0, 1),
                    PolicyConfig.DEFAULT);
            assertEquals(55.0, out.result().foir());
            assertEquals(Verdict.BORDERLINE, out.result().verdict());
        }

        @Test
        @DisplayName("NOT_ELIGIBLE above 55%")
        void notEligible() {
            ScreenOutcome out = FoirEngine.screen(inputs(100000, 60000, 1, 0, 1),
                    PolicyConfig.DEFAULT);
            assertEquals(Verdict.NOT_ELIGIBLE, out.result().verdict());
            assertEquals("REJECT_FOIR_HIGH", out.result().reasonCode());
        }

        @Test
        @DisplayName("respects custom (configurable) thresholds")
        void customPolicy() {
            PolicyConfig strict = new PolicyConfig(35, 45, RoundingMode.HALF_EVEN);
            ScreenOutcome out = FoirEngine.screen(inputs(100000, 40000, 1, 0, 1), strict);
            assertEquals(Verdict.BORDERLINE, out.result().verdict()); // 40% between 35 and 45
        }
    }

    @Nested
    @DisplayName("precision — no integer division")
    class Precision {

        @Test
        @DisplayName("keeps a fractional FOIR instead of truncating to an int")
        void fractionalFoir() {
            ScreenOutcome out = FoirEngine.screen(inputs(90000, 12400, 500000, 10, 60),
                    PolicyConfig.DEFAULT);
            assertTrue(out.ok());
            assertEquals(25.6, out.result().foir(), 1e-9);
            assertEquals(10623.52, out.result().newEmi());
            assertFalse(out.result().foirExact() == Math.floor(out.result().foirExact()));
        }
    }

    @Nested
    @DisplayName("validation — zero / negative guards never crash")
    class Validation {

        @Test
        @DisplayName("zero income returns an error, does not throw")
        void zeroIncome() {
            ScreenOutcome out = FoirEngine.screen(inputs(0, 0, 500000, 10, 60),
                    PolicyConfig.DEFAULT);
            assertFalse(out.ok());
            assertEquals("ERR_INCOME_NONPOSITIVE", out.error().code());
        }

        @Test
        @DisplayName("negative and NaN income rejected")
        void badIncome() {
            assertEquals("ERR_INCOME_NONPOSITIVE",
                    FoirEngine.validate(inputs(-5000, 0, 500000, 10, 60)).code());
            assertEquals("ERR_INCOME_NONPOSITIVE",
                    FoirEngine.validate(inputs(Double.NaN, 0, 500000, 10, 60)).code());
        }

        @Test
        @DisplayName("negative existing EMI rejected")
        void negativeExisting() {
            assertEquals("ERR_EMI_NEGATIVE",
                    FoirEngine.validate(inputs(100000, -1, 500000, 10, 60)).code());
        }

        @Test
        @DisplayName("zero loan amount rejected")
        void zeroLoan() {
            assertEquals("ERR_LOAN_NONPOSITIVE",
                    FoirEngine.validate(inputs(100000, 0, 0, 10, 60)).code());
        }

        @Test
        @DisplayName("negative rate rejected, zero rate allowed")
        void rateGuards() {
            assertEquals("ERR_RATE_NEGATIVE",
                    FoirEngine.validate(inputs(100000, 0, 500000, -1, 60)).code());
            assertNull(FoirEngine.validate(inputs(100000, 0, 500000, 0, 60)));
        }

        @Test
        @DisplayName("zero tenure rejected")
        void zeroTenure() {
            assertEquals("ERR_TENURE_NONPOSITIVE",
                    FoirEngine.validate(inputs(100000, 0, 500000, 10, 0)).code());
        }

        @Test
        @DisplayName("a fully valid input set passes")
        void validPasses() {
            assertNull(FoirEngine.validate(inputs(100000, 0, 500000, 10, 60)));
            assertNotNull(PolicyConfig.DEFAULT);
        }
    }
}

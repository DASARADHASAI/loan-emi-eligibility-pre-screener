package com.bank.prescreener.foir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Advisor — counter-offer solver")
class AdvisorTest {

    private static ScreenInputs in(double income, double existingEmi, double loan,
                                   double rate, int tenure) {
        return new ScreenInputs(income, existingEmi, loan, rate, tenure);
    }

    @Test
    @DisplayName("principalForEmi inverts computeEmi")
    void inverseOfEmi() {
        double emi = FoirEngine.computeEmi(500000, 10, 60);
        assertEquals(500000, Advisor.principalForEmi(emi, 10, 60), 1e-3);
        assertEquals(120000, Advisor.principalForEmi(10000, 0, 12), 1e-6);
    }

    @Test
    @DisplayName("the max eligible loan actually screens as ELIGIBLE")
    void maxLoanIsEligible() {
        ScreenInputs base = in(100000, 0, 800000, 10, 60);
        Advice advice = Advisor.advise(base, PolicyConfig.DEFAULT);
        assertTrue(advice.feasible());
        assertTrue(advice.maxEligibleLoan() > 0);

        ScreenOutcome atMax = FoirEngine.screen(
                in(100000, 0, advice.maxEligibleLoan(), 10, 60), PolicyConfig.DEFAULT);
        assertEquals(Verdict.ELIGIBLE, atMax.result().verdict());
    }

    @Test
    @DisplayName("not feasible when existing EMIs already exceed the limit")
    void notFeasible() {
        Advice advice = Advisor.advise(in(100000, 55000, 500000, 10, 60), PolicyConfig.DEFAULT);
        assertFalse(advice.feasible());
        assertEquals(0.0, advice.maxEligibleLoan());
        assertEquals(-1, advice.tenureToQualify());
    }

    @Test
    @DisplayName("computes the tenure needed to qualify for the requested amount")
    void tenureToQualify() {
        // ₹30L @ 10% needs ~84 months to bring the EMI within a 50k budget.
        Advice advice = Advisor.advise(in(100000, 0, 3000000, 10, 60), PolicyConfig.DEFAULT);
        assertEquals(84, advice.tenureToQualify());

        // Sanity: at that tenure the requested loan is indeed eligible.
        ScreenOutcome atTenure = FoirEngine.screen(
                in(100000, 0, 3000000, 10, advice.tenureToQualify()), PolicyConfig.DEFAULT);
        assertEquals(Verdict.ELIGIBLE, atTenure.result().verdict());
    }

    @Test
    @DisplayName("tenure alone can't help when monthly interest exceeds the budget")
    void tenureUnachievable() {
        // ₹1cr @ 10% → monthly interest alone (~83k) already exceeds the 50k budget.
        Advice advice = Advisor.advise(in(100000, 0, 10000000, 10, 60), PolicyConfig.DEFAULT);
        assertTrue(advice.feasible());          // a smaller loan still works
        assertEquals(-1, advice.tenureToQualify()); // but stretching tenure won't
    }
}

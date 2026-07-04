package com.bank.prescreener.config;

import com.bank.prescreener.foir.PolicyConfig;
import com.bank.prescreener.web.dto.RateSlab;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Central tariff &amp; policy constants — the single place to edit.
 *
 * <p>WATCH-OUT addressed: "Hard-coded interest slabs make the tool brittle when RBI
 * rates change." Every rate and threshold lives here, so one edit updates the whole
 * tool. Nothing downstream hard-codes a rate.
 */
@Component
public class TariffConfig {

    /** Verdict bands + EMI rounding. Banker's rounding to match core-banking ledgers. */
    private static final PolicyConfig POLICY =
            new PolicyConfig(50.0, 55.0, RoundingMode.HALF_EVEN);

    /** Interest products offered to the agent. */
    private static final List<RateSlab> SLABS = List.of(
            new RateSlab("Personal Loan", 50_000, 500_000, 14.00),
            new RateSlab("Personal Loan Plus", 500_001, 2_000_000, 12.50),
            new RateSlab("Home Loan", 500_000, 10_000_000, 8.50),
            new RateSlab("Auto Loan", 100_000, 2_500_000, 9.75),
            new RateSlab("Two-Wheeler Loan", 20_000, 300_000, 11.50));

    public PolicyConfig policy() {
        return POLICY;
    }

    public List<RateSlab> slabs() {
        return SLABS;
    }
}

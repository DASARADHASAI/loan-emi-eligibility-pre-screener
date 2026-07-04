package com.bank.prescreener.service;

import com.bank.prescreener.config.TariffConfig;
import com.bank.prescreener.foir.Advisor;
import com.bank.prescreener.foir.FoirEngine;
import com.bank.prescreener.foir.PolicyConfig;
import com.bank.prescreener.foir.ScreenError;
import com.bank.prescreener.foir.ScreenInputs;
import com.bank.prescreener.foir.ScreenOutcome;
import com.bank.prescreener.model.Screening;
import com.bank.prescreener.repo.ScreeningRepository;
import com.bank.prescreener.web.dto.CompareItem;
import com.bank.prescreener.web.dto.CompareRequest;
import com.bank.prescreener.web.dto.CompareResponse;
import com.bank.prescreener.web.dto.RateSlab;
import com.bank.prescreener.web.dto.ScreenRequest;
import com.bank.prescreener.web.dto.ScreenResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Maps HTTP requests onto the pure engine + advisor, and records each screening. */
@Service
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);

    private final TariffConfig tariffs;
    private final ScreeningRepository screenings;

    public ScreeningService(TariffConfig tariffs, ScreeningRepository screenings) {
        this.tariffs = tariffs;
        this.screenings = screenings;
    }

    /** Screen one applicant, record the result, and attach a counter-offer. */
    public ScreenResponse screen(ScreenRequest req, String agentEmail) {
        ScreenInputs inputs = new ScreenInputs(
                orNaN(req.income()),
                req.existingEmi() == null ? 0.0 : req.existingEmi(),
                orNaN(req.loanAmount()),
                orNaN(req.annualRatePct()),
                req.tenureMonths() == null ? 0 : req.tenureMonths());

        PolicyConfig policy = tariffs.policy();
        ScreenOutcome outcome = FoirEngine.screen(inputs, policy);

        if (!outcome.ok()) {
            return new ScreenResponse(false, null, outcome.error(), null);
        }
        persist(inputs, outcome, agentEmail);
        return new ScreenResponse(true, outcome.result(), null, Advisor.advise(inputs, policy));
    }

    /** Screen the same applicant against every product. */
    public CompareResponse compare(CompareRequest req, String agentEmail) {
        double income = orNaN(req.income());
        double existing = req.existingEmi() == null ? 0.0 : req.existingEmi();
        double loan = orNaN(req.loanAmount());
        int tenure = req.tenureMonths() == null ? 0 : req.tenureMonths();

        // Validate the shared inputs once (rate is supplied per product, so use 0 here).
        ScreenError error = FoirEngine.validate(new ScreenInputs(income, existing, loan, 0, tenure));
        if (error != null) {
            return new CompareResponse(false, error, List.of());
        }

        PolicyConfig policy = tariffs.policy();
        List<CompareItem> items = new ArrayList<>();
        for (RateSlab slab : tariffs.slabs()) {
            ScreenOutcome out = FoirEngine.screen(
                    new ScreenInputs(income, existing, loan, slab.annualRate(), tenure), policy);
            var r = out.result();
            boolean within = loan >= slab.minAmount() && loan <= slab.maxAmount();
            items.add(new CompareItem(
                    slab.product(), slab.annualRate(), slab.minAmount(), slab.maxAmount(),
                    within, r.newEmi(), r.foir(), r.verdict().name(), r.reasonCode()));
        }
        return new CompareResponse(true, null, items);
    }

    private void persist(ScreenInputs in, ScreenOutcome outcome, String agentEmail) {
        try {
            Screening row = new Screening();
            row.setAgentEmail(agentEmail);
            row.setIncome(in.income());
            row.setExistingEmi(in.existingEmi());
            row.setLoanAmount(in.loanAmount());
            row.setAnnualRate(in.annualRatePct());
            row.setTenureMonths(in.tenureMonths());
            row.setNewEmi(outcome.result().newEmi());
            row.setFoir(outcome.result().foir());
            row.setVerdict(outcome.result().verdict().name());
            row.setReasonCode(outcome.result().reasonCode());
            screenings.save(row);
        } catch (Exception e) {
            log.warn("Could not record screening (verdict still returned): {}", e.getMessage());
        }
    }

    private static double orNaN(Double d) {
        return d == null ? Double.NaN : d;
    }
}

package com.bank.prescreener.web;

import com.bank.prescreener.config.TariffConfig;
import com.bank.prescreener.foir.PolicyConfig;
import com.bank.prescreener.web.dto.PolicyView;
import java.math.RoundingMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The FOIR policy the dial paints its zones from. */
@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final TariffConfig tariffs;

    public PolicyController(TariffConfig tariffs) {
        this.tariffs = tariffs;
    }

    @GetMapping
    public PolicyView current() {
        PolicyConfig p = tariffs.policy();
        String mode = p.roundingMode() == RoundingMode.HALF_UP ? "half_up" : "bankers";
        return new PolicyView(p.eligibleMax(), p.borderlineMax(), mode);
    }
}

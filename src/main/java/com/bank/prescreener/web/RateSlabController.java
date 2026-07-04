package com.bank.prescreener.web;

import com.bank.prescreener.config.TariffConfig;
import com.bank.prescreener.web.dto.RateSlab;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Interest products for the screener's dropdown (auto-fills the rate). */
@RestController
@RequestMapping("/api/rate-slabs")
public class RateSlabController {

    private final TariffConfig tariffs;

    public RateSlabController(TariffConfig tariffs) {
        this.tariffs = tariffs;
    }

    @GetMapping
    public List<RateSlab> all() {
        return tariffs.slabs();
    }
}

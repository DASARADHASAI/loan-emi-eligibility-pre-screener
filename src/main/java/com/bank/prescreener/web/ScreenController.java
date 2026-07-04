package com.bank.prescreener.web;

import com.bank.prescreener.service.ScreeningService;
import com.bank.prescreener.web.dto.ScreenRequest;
import com.bank.prescreener.web.dto.ScreenResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** USE CASES 1–4 (+ counter-offer): financials → verdict + EMI + reason + what they CAN get. */
@RestController
@RequestMapping("/api")
public class ScreenController {

    private final ScreeningService service;

    public ScreenController(ScreeningService service) {
        this.service = service;
    }

    @PostMapping("/screen")
    public ScreenResponse screen(@RequestBody ScreenRequest request, Principal principal) {
        return service.screen(request, principal.getName());
    }
}

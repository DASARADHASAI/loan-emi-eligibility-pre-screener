package com.bank.prescreener.web;

import com.bank.prescreener.service.ScreeningService;
import com.bank.prescreener.web.dto.CompareRequest;
import com.bank.prescreener.web.dto.CompareResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Screen one applicant across every loan product at once. */
@RestController
@RequestMapping("/api")
public class CompareController {

    private final ScreeningService service;

    public CompareController(ScreeningService service) {
        this.service = service;
    }

    @PostMapping("/compare")
    public CompareResponse compare(@RequestBody CompareRequest request, Principal principal) {
        return service.compare(request, principal.getName());
    }
}

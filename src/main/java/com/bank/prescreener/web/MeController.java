package com.bank.prescreener.web;

import com.bank.prescreener.repo.UserRepository;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in agent, for the app header ("Hi, {name}"). */
@RestController
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/me")
    public Map<String, String> me(Principal principal) {
        String email = principal.getName();
        String name = users.findByEmail(email).map(u -> u.getName()).orElse("Agent");
        return Map.of("name", name, "email", email);
    }
}

package com.bank.prescreener.web;

import com.bank.prescreener.model.AppUser;
import com.bank.prescreener.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Handles account creation. Login/logout are handled by Spring Security. */
@Controller
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Serves the signup page (POST /signup below creates the account). */
    @GetMapping("/signup")
    public String signupPage() {
        return "forward:/signup.html";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String name,
                         @RequestParam String email,
                         @RequestParam String password) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isEmpty() || cleanEmail.isEmpty()) {
            return "redirect:/signup?error=missing";
        }
        if (password == null || password.length() < 6) {
            return "redirect:/signup?error=weak";
        }
        if (users.findByEmail(cleanEmail).isPresent()) {
            return "redirect:/signup?error=exists";
        }

        users.save(new AppUser(cleanName, cleanEmail, encoder.encode(password)));
        return "redirect:/login?registered=1";
    }
}

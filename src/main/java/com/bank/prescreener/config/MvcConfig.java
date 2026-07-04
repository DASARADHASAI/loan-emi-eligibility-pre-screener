package com.bank.prescreener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Clean URLs that serve the static pages. */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/login.html");
        registry.addViewController("/app").setViewName("forward:/app.html");
        registry.addViewController("/history").setViewName("forward:/history.html");
        // NOTE: /signup GET is served by AuthController (it also handles POST /signup),
        // so it must not be a view controller here or GET would 405 against the POST.
    }
}

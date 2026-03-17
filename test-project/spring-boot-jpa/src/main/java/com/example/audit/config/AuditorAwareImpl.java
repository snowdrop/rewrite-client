package com.example.audit.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // In a real application, retrieve the currently logged-in user
        // from Spring Security's SecurityContextHolder.
        return Optional.of("system");
    }
}
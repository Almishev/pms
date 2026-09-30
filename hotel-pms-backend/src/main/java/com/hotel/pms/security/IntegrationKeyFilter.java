package com.hotel.pms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class IntegrationKeyFilter extends OncePerRequestFilter {
    @Value("${pms.integration.key:}")
    private String integrationKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/integration");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!keyMatches(request.getHeader("X-Integration-Key"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Невалиден интеграционен ключ\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean keyMatches(String provided) {
        if (integrationKey == null || integrationKey.isBlank() || provided == null || provided.isBlank()) {
            return false;
        }
        byte[] expected = integrationKey.trim().getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}

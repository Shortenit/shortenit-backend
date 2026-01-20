package edu.au.life.shortenit.security;

import edu.au.life.shortenit.entity.ApiKey;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * ApiKeyAuthenticationFilter - FINAL WORKING VERSION
 * Uses Spring's PasswordEncoder instead of BCrypt directly
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApplicationContext applicationContext;
    private ApiKeyRepository apiKeyRepository;
    private PasswordEncoder passwordEncoder;

    public ApiKeyAuthenticationFilter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private ApiKeyRepository getApiKeyRepository() {
        if (apiKeyRepository == null) {
            apiKeyRepository = applicationContext.getBean(ApiKeyRepository.class);
        }
        return apiKeyRepository;
    }

    private PasswordEncoder getPasswordEncoder() {
        if (passwordEncoder == null) {
            passwordEncoder = applicationContext.getBean(PasswordEncoder.class);
        }
        return passwordEncoder;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        System.out.println("🔍 [ApiKeyFilter] Processing: " + request.getMethod() + " " + path);

        // Skip API key validation for public endpoints
        if (path.startsWith("/s/") ||
                path.startsWith("/api/auth/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/test/") ||  // Allow test endpoint
                path.equals("/error")) {
            System.out.println("✅ [ApiKeyFilter] Public endpoint - skipping");
            filterChain.doFilter(request, response);
            return;
        }

        final String apiKeyHeader = request.getHeader("X-API-Key");

        System.out.println("🔑 [ApiKeyFilter] X-API-Key header: " + (apiKeyHeader != null ? "PRESENT ('" + apiKeyHeader + "')" : "MISSING"));

        if (apiKeyHeader == null || apiKeyHeader.isEmpty()) {
            System.out.println("⚠️  [ApiKeyFilter] No API key - continuing to next filter");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            System.out.println("🔍 [ApiKeyFilter] Checking API key against database...");

            // Get all API keys and check with PasswordEncoder
            List<ApiKey> allKeys = getApiKeyRepository().findAllWithUser();
            System.out.println("📊 [ApiKeyFilter] Found " + allKeys.size() + " API keys in database");

            ApiKey matchedKey = null;

            for (ApiKey key : allKeys) {
                System.out.println("🔐 [ApiKeyFilter] Checking key ID " + key.getId());
                System.out.println("    Plain text: '" + apiKeyHeader + "'");
                System.out.println("    Hash from DB: " + key.getKeyHash());

                boolean matches = getPasswordEncoder().matches(apiKeyHeader, key.getKeyHash());
                System.out.println("    Result: " + (matches ? "✅ MATCH!" : "❌ no match"));

                if (matches) {
                    matchedKey = key;
                    break;
                }
            }

            if (matchedKey != null) {
                System.out.println("✅ [ApiKeyFilter] API key validated! User ID: " + matchedKey.getUser().getId());

                // Check expiration
                if (matchedKey.getExpiresAt() != null &&
                        matchedKey.getExpiresAt().isBefore(LocalDateTime.now())) {
                    System.out.println("❌ [ApiKeyFilter] API key expired!");
                    filterChain.doFilter(request, response);
                    return;
                }

                User user = matchedKey.getUser();

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                        );

                authToken.setDetails(new JwtAuthenticationFilter.UserPrincipal(
                        user.getId(), user.getEmail(), user.getRole().name()));

                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("✅ [ApiKeyFilter] Authentication set in SecurityContext!");

                // Update last used
                matchedKey.setLastUsedAt(LocalDateTime.now());
                getApiKeyRepository().save(matchedKey);
            } else {
                System.out.println("❌ [ApiKeyFilter] API key NOT found in database!");
            }
        } catch (Exception e) {
            System.out.println("💥 [ApiKeyFilter] Error: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}
package edu.au.life.shortenit.security;

import edu.au.life.shortenit.config.ProtectedAdminConfig;  // ← NEW
import edu.au.life.shortenit.entity.RefreshToken;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.repository.UserRepository;
import edu.au.life.shortenit.service.JwtService;
import edu.au.life.shortenit.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.microsoft.client-id")
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final ProtectedAdminConfig protectedAdminConfig;  // ← NEW

    @Value("${auth.allowed-email-domain:au.edu}")
    private String allowedEmailDomain;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public OAuth2SuccessHandler(JwtService jwtService,
                                RefreshTokenService refreshTokenService,
                                UserRepository userRepository,
                                ProtectedAdminConfig protectedAdminConfig) {  // ← NEW PARAM
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.protectedAdminConfig = protectedAdminConfig;  // ← NEW
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Get OAuth2 user info
        Object principal = authentication.getPrincipal();
        String microsoftId = null;
        String email = null;
        String name = null;

        if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            microsoftId = oidcUser.getAttribute("sub");
            email = oidcUser.getAttribute("email");
            name = oidcUser.getAttribute("name");
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            microsoftId = oAuth2User.getAttribute("id");
            email = oAuth2User.getAttribute("mail");
            if (email == null) {
                email = oAuth2User.getAttribute("userPrincipalName");
            }
            name = oAuth2User.getAttribute("displayName");
        }

        log.info("OAuth2 login attempt for email: {}", email);

        // VALIDATE EMAIL DOMAIN (University only!)
        if (email == null || !email.endsWith("@" + allowedEmailDomain)) {
            log.warn("OAuth2 login rejected: email domain not allowed. Required: @{}, Got: {}", allowedEmailDomain, email);

            // Redirect to frontend error page
            String errorUrl = UriComponentsBuilder.fromUriString(baseUrl + "/auth/error")
                    .queryParam("error", "invalid_email_domain")
                    .queryParam("message", "Only " + allowedEmailDomain + " emails are allowed")
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        // Make variables effectively final for lambda
        final String finalMicrosoftId = microsoftId;
        final String finalEmail = email;
        final String finalName = name;

        // Find or create user
        User user = userRepository.findByMicrosoftId(finalMicrosoftId)
                .orElseGet(() -> {
                    // ← NEW: Check if this email should be auto-promoted to ADMIN
                    User.Role assignedRole;
                    if (protectedAdminConfig.isProtectedAdminEmail(finalEmail)) {
                        assignedRole = User.Role.ADMIN;
                        log.info("Auto-promoting new user {} to ADMIN (protected admin email)", finalEmail);
                    } else {
                        assignedRole = User.Role.USER;
                        log.info("Creating new user for email: {}", finalEmail);
                    }

                    User newUser = new User();
                    newUser.setMicrosoftId(finalMicrosoftId);
                    newUser.setEmail(finalEmail);
                    newUser.setName(finalName);
                    newUser.setRole(assignedRole);  // ← CHANGED: was hardcoded User.Role.USER
                    User saved = userRepository.save(newUser);
                    log.info("User created with ID: {}, role: {}", saved.getId(), saved.getRole());
                    return saved;
                });

        // Update last login
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("OAuth2 login successful for user ID: {}, role: {}", user.getId(), user.getRole());

        // Build redirect URL with tokens - REDIRECT TO FRONTEND
        String targetUrl = UriComponentsBuilder.fromUriString(baseUrl + "/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .queryParam("user", user.getEmail())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
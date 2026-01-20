package edu.au.life.shortenit.security;

import edu.au.life.shortenit.entity.RefreshToken;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.repository.UserRepository;
import edu.au.life.shortenit.service.JwtService;
import edu.au.life.shortenit.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.microsoft.client-id")
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Value("${auth.allowed-email-domain:au.edu}")
    private String allowedEmailDomain;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public OAuth2SuccessHandler(JwtService jwtService,
                                RefreshTokenService refreshTokenService,
                                UserRepository userRepository) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
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

        System.out.println("=".repeat(60));
        System.out.println("🔐 OAuth2 Login Attempt");
        System.out.println("   Microsoft ID: " + microsoftId);
        System.out.println("   Email: " + email);
        System.out.println("   Name: " + name);

        // VALIDATE EMAIL DOMAIN (University only!)
        if (email == null || !email.endsWith("@" + allowedEmailDomain)) {
            System.out.println("❌ REJECTED: Email domain not allowed");
            System.out.println("   Required: @" + allowedEmailDomain);
            System.out.println("   Got: " + email);
            System.out.println("=".repeat(60));

            // Redirect to error page with message
            String errorUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth2/error.html")
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
                    System.out.println("👤 Creating new user...");
                    User newUser = new User();
                    newUser.setMicrosoftId(finalMicrosoftId);
                    newUser.setEmail(finalEmail);
                    newUser.setName(finalName);
                    newUser.setRole(User.Role.USER);
                    User saved = userRepository.save(newUser);
                    System.out.println("✅ User created with ID: " + saved.getId());
                    return saved;
                });

        // Update last login
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        System.out.println("✅ OAuth2 login successful!");
        System.out.println("   User ID: " + user.getId());
        System.out.println("   Role: " + user.getRole());
        System.out.println("   Access Token: " + accessToken.substring(0, 30) + "...");
        System.out.println("   Refresh Token: " + refreshToken.getToken());
        System.out.println("=".repeat(60));

        // Build redirect URL with tokens
        String targetUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth2/success.html")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .queryParam("user", user.getEmail())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
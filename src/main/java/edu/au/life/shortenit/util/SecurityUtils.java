package edu.au.life.shortenit.util;

import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.exception.ResourceNotFoundException;
import edu.au.life.shortenit.exception.UnauthorizedException;
import edu.au.life.shortenit.repository.UserRepository;
import edu.au.life.shortenit.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository repo) {
        SecurityUtils.userRepository = repo;
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }

        Object details = authentication.getDetails();

        if (details instanceof JwtAuthenticationFilter.UserPrincipal) {
            JwtAuthenticationFilter.UserPrincipal principal =
                    (JwtAuthenticationFilter.UserPrincipal) details;

            if (userRepository == null) {
                throw new IllegalStateException("SecurityUtils not properly initialized");
            }

            return userRepository.findById(principal.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        throw new UnauthorizedException("Invalid authentication");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}

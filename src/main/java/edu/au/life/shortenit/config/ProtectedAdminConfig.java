package edu.au.life.shortenit.config;

import edu.au.life.shortenit.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProtectedAdminConfig {

    private final Set<String> protectedAdminEmails;

    public ProtectedAdminConfig(@Value("${admin.protected.email}") String protectedAdminEmail) {
        this.protectedAdminEmails = Arrays.stream(protectedAdminEmail.split(","))
                .map(email -> email.toLowerCase().trim())
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean isProtectedAdmin(User user) {
        if (user == null || user.getEmail() == null) {
            return false;
        }
        return protectedAdminEmails.contains(user.getEmail().toLowerCase().trim());
    }

    public boolean isProtectedAdminEmail(String email) {
        if (email == null) {
            return false;
        }
        return protectedAdminEmails.contains(email.toLowerCase().trim());
    }

    public Set<String> getProtectedAdminEmails() {
        return protectedAdminEmails;
    }
}
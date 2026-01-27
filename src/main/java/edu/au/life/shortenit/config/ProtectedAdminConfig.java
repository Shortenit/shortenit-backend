package edu.au.life.shortenit.config;

import edu.au.life.shortenit.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProtectedAdminConfig {

    private final String protectedAdminEmail;

    public ProtectedAdminConfig(@Value("${admin.protected.email}") String protectedAdminEmail) {
        this.protectedAdminEmail = protectedAdminEmail.toLowerCase().trim();
    }

    public boolean isProtectedAdmin(User user) {
        if (user == null || user.getEmail() == null) {
            return false;
        }
        return user.getEmail().toLowerCase().trim().equals(protectedAdminEmail);
    }

    public boolean isProtectedAdminEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.toLowerCase().trim().equals(protectedAdminEmail);
    }

    public String getProtectedAdminEmail() {
        return protectedAdminEmail;
    }
}

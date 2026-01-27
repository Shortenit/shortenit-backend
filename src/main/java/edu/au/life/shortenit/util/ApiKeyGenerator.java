package edu.au.life.shortenit.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public String generateApiKey(String prefix, int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);

        String randomPart = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes)
                .substring(0, length);

        return prefix + randomPart;
    }

}

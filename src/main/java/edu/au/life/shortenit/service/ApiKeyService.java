package edu.au.life.shortenit.service;

import edu.au.life.shortenit.dto.ApiKeyRequest;
import edu.au.life.shortenit.dto.ApiKeyResponse;
import edu.au.life.shortenit.entity.ApiKey;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.exception.ForbiddenException;
import edu.au.life.shortenit.exception.ResourceNotFoundException;
import edu.au.life.shortenit.repository.ApiKeyRepository;
import edu.au.life.shortenit.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyGenerator apiKeyGenerator;

    @Value("${api-key.prefix}")
    private String apiKeyPrefix;

    @Value("${api-key.length}")
    private int apiKeyLength;

    @Transactional
    public ApiKeyResponse createApiKey(User user, ApiKeyRequest request) {

        String apiKey = apiKeyGenerator.generateApiKey(apiKeyPrefix, apiKeyLength);
        String keyHash = BCrypt.hashpw(apiKey, BCrypt.gensalt(10));

        LocalDateTime expiresAt = null;
        if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpirationDays());
        }

        ApiKey entity = ApiKey.builder()
                .user(user)
                .keyHash(keyHash)
                .name(request.getName())
                .expiresAt(expiresAt)
                .build();

        entity = apiKeyRepository.save(entity);

        return ApiKeyResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .apiKey(apiKey)
                .maskedKey(maskApiKey(apiKey))
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<ApiKeyResponse> getUserApiKeys(User user) {
        List<ApiKey> apiKeys = apiKeyRepository.findByUserOrderByCreatedAtDesc(user);

        return apiKeys.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteApiKey(User user, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        // Check ownership
        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to delete this API key");
        }

        apiKeyRepository.delete(apiKey);
    }

    private ApiKeyResponse convertToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .apiKey(null)
                .maskedKey("sk_live_***" + apiKey.getId())
                .scopes(apiKey.getScopes())
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) {
            return "***";
        }
        String start = apiKey.substring(0, 8);
        String end = apiKey.substring(apiKey.length() - 4);
        return start + "..." + end;
    }
}

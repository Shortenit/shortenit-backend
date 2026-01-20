package edu.au.life.shortenit.service;

import edu.au.life.shortenit.entity.RefreshToken;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.exception.ResourceNotFoundException;
import edu.au.life.shortenit.repository.RefreshTokenRepository;
import edu.au.life.shortenit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Find refresh token by token string
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Create a new refresh token for user
     */
    public RefreshToken createRefreshToken(Long userId) {
        // Find user first
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Calculate expiration time
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(refreshTokenDurationMs / 1000);

        // Build and save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(expiryDate)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify if refresh token is expired
     * Throws exception if expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException(
                    "Refresh token has expired. Please login again."
            );
        }
        return token;
    }

    /**
     * Check if user has any active refresh token
     */
    public boolean hasActiveToken(User user) {
        return refreshTokenRepository.hasActiveToken(user, LocalDateTime.now());
    }

    /**
     * Delete all refresh tokens for a user (logout)
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Delete all refresh tokens for a user by user ID
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Delete a specific refresh token
     */
    @Transactional
    public void deleteByToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
        refreshTokenRepository.delete(refreshToken);
    }

    /**
     * Scheduled task to clean up expired tokens
     * Runs every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpired(now);
        System.out.println("🧹 Cleaned up expired refresh tokens at: " + now);
    }
}
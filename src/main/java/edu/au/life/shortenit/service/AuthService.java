package edu.au.life.shortenit.service;

import edu.au.life.shortenit.dto.LoginResponse;
import edu.au.life.shortenit.entity.RefreshToken;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.repository.RefreshTokenRepository;
import edu.au.life.shortenit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import edu.au.life.shortenit.exception.InvalidTokenException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public LoginResponse loginWithMicrosoft(String email, String name, String microsoftId) {
        // find or create user
        User user = userRepository.findByMicrosoftId(microsoftId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .microsoftId(microsoftId)
                            .name(name)
                            .role(User.Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });

        // update user
        if(!user.getEmail().equals(email) || !user.getName().equals(name)) {
            user.setEmail(email);
            user.setName(name);
            userRepository.save(user);
        }
        return generateTokensForUser(user);
    }

    private LoginResponse generateTokensForUser(User user) {

        refreshTokenRepository.deleteByUser(user);
        // Generate JWT Token
        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshTokenString = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration/1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build()).build();

    }

    @Transactional
    public LoginResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token."));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired.");
        }

        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name()).build()).build();
    }

    @Transactional
    public void logout (User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}

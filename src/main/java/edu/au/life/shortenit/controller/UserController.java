package edu.au.life.shortenit.controller;


import edu.au.life.shortenit.dto.*;
import edu.au.life.shortenit.entity.ApiKey;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.repository.UserRepository;
import edu.au.life.shortenit.service.ApiKeyService;
import edu.au.life.shortenit.service.UserService;
import edu.au.life.shortenit.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private ApiKeyService apiKeyService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        User currentUser = SecurityUtils.getCurrentUser();
        UserResponse response = userService.getUserProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        UserResponse response = userService.updateUserProfile(currentUser, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/api-keys")
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody ApiKeyRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        ApiKeyResponse response = apiKeyService.createApiKey(currentUser, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/api-keys")
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        User currentUser = SecurityUtils.getCurrentUser();
        List<ApiKeyResponse> response = apiKeyService.getUserApiKeys(currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/api-keys/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        apiKeyService.deleteApiKey(currentUser, id);
        return ResponseEntity.ok().build();
    }
}

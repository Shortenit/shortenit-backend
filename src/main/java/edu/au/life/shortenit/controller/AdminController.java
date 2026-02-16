package edu.au.life.shortenit.controller;

import edu.au.life.shortenit.dto.AnalyticsResponse;
import edu.au.life.shortenit.dto.DashboardStatsResponse;
import edu.au.life.shortenit.dto.RoleUpdateRequest;
import edu.au.life.shortenit.dto.UrlResponse;
import edu.au.life.shortenit.dto.UserResponse;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.exception.ResourceNotFoundException;
import edu.au.life.shortenit.repository.UserRepository;
import edu.au.life.shortenit.service.AdminService;
import edu.au.life.shortenit.service.AnalyticsService;
import edu.au.life.shortenit.service.UrlService;
import edu.au.life.shortenit.util.SecurityUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, UrlService urlService, AnalyticsService analyticsService, UserRepository userRepository) {
        this.adminService = adminService;
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        adminService.deleteUser(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        UserResponse response = adminService.updateUserRole(currentUser, id, request.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> promoteUser(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        UserResponse response = adminService.promoteToAdmin(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/demote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> demoteUser(@PathVariable Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        UserResponse response = adminService.demoteToUser(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/protected-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProtectedStatusResponse> getProtectedStatus() {
        User currentUser = SecurityUtils.getCurrentUser();
        boolean isProtected = adminService.isProtectedAdmin(currentUser);
        return ResponseEntity.ok(new ProtectedStatusResponse(isProtected));
    }

    // ==================== Admin Stats Endpoint ====================

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardStatsResponse> getAdminStats() {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Admin {} viewing dashboard stats", currentUser.getEmail());
        DashboardStatsResponse stats = urlService.getAdminDashboardStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== Admin Analytics Endpoints ====================

    @GetMapping("/analytics/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String code) {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Admin {} viewing analytics for code: {}", currentUser.getEmail(), code);
        AnalyticsResponse analytics = analyticsService.getAnalyticsAdmin(code);
        return ResponseEntity.ok(analytics);
    }

    // ==================== Admin URL Endpoints ====================

    @GetMapping("/urls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UrlResponse>> getAllUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Admin {} listing all URLs", currentUser.getEmail());
        Page<UrlResponse> response = urlService.getAllUrlsPaginatedAdmin(page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/urls/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AnalyticsResponse>> getAllUrlsWithAnalytics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        User currentUser = SecurityUtils.getCurrentUser();
        log.info("Admin {} listing all URLs with analytics", currentUser.getEmail());
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<AnalyticsResponse> response = analyticsService.getAllAnalyticsPaginatedAdmin(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/urls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UrlResponse>> getUrlsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        User currentUser = SecurityUtils.getCurrentUser();

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        log.info("Admin {} listing URLs for user ID: {}", currentUser.getEmail(), userId);
        Page<UrlResponse> response = urlService.getUrlsByUserIdPaginated(userId, page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/urls/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AnalyticsResponse>> getUrlsByUserIdWithAnalytics(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        User currentUser = SecurityUtils.getCurrentUser();

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        log.info("Admin {} listing URLs with analytics for user ID: {}", currentUser.getEmail(), userId);
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<AnalyticsResponse> response = analyticsService.getAnalyticsByUserIdPaginated(userId, pageable);
        return ResponseEntity.ok(response);
    }

    public record ProtectedStatusResponse(boolean isProtectedAdmin) {}
}

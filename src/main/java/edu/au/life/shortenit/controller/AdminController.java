package edu.au.life.shortenit.controller;

import edu.au.life.shortenit.dto.RoleUpdateRequest;
import edu.au.life.shortenit.dto.UserResponse;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.service.AdminService;
import edu.au.life.shortenit.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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

    public record ProtectedStatusResponse(boolean isProtectedAdmin) {}
}

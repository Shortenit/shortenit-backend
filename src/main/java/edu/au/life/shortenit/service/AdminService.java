package edu.au.life.shortenit.service;

import edu.au.life.shortenit.config.ProtectedAdminConfig;
import edu.au.life.shortenit.dto.UserResponse;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.entity.User.Role;
import edu.au.life.shortenit.exception.ForbiddenException;
import edu.au.life.shortenit.exception.ResourceNotFoundException;
import edu.au.life.shortenit.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProtectedAdminConfig protectedAdminConfig;

    public AdminService(UserRepository userRepository, ProtectedAdminConfig protectedAdminConfig) {
        this.userRepository = userRepository;
        this.protectedAdminConfig = protectedAdminConfig;
    }

    public boolean isProtectedAdmin(User user) {
        return protectedAdminConfig.isProtectedAdmin(user);
    }

    public boolean canDelete(User actor, User target) {
        // No one can delete the protected admin
        if (isProtectedAdmin(target)) {
            return false;
        }

        // USER cannot delete anyone
        if (actor.getRole() == Role.USER) {
            return false;
        }

        // Protected admin can delete anyone (except themselves - handled above)
        if (isProtectedAdmin(actor)) {
            return true;
        }

        // Normal ADMIN can only delete USER, not other ADMINs
        if (actor.getRole() == Role.ADMIN) {
            return target.getRole() == Role.USER;
        }

        return false;
    }

    public boolean canModifyRole(User actor, User target, Role newRole) {
        // Only ADMINs can modify roles
        if (actor.getRole() != Role.ADMIN) {
            return false;
        }

        // Protected admin cannot be demoted
        if (isProtectedAdmin(target) && newRole != Role.ADMIN) {
            return false;
        }

        // Normal admin cannot promote/demote other admins (except protected admin can)
        if (!isProtectedAdmin(actor) && target.getRole() == Role.ADMIN) {
            return false;
        }

        return true;
    }

    @Transactional
    public void deleteUser(User actor, Long targetId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetId));

        // Cannot delete yourself
        if (actor.getId().equals(target.getId())) {
            throw new ForbiddenException("Cannot delete your own account");
        }

        // Check if protected admin is being targeted
        if (isProtectedAdmin(target)) {
            throw new ForbiddenException("Cannot delete the protected admin");
        }

        // USER cannot delete anyone
        if (actor.getRole() == Role.USER) {
            throw new ForbiddenException("Users are not authorized to delete other users");
        }

        // Normal ADMIN cannot delete other ADMINs
        if (!isProtectedAdmin(actor) && actor.getRole() == Role.ADMIN && target.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Admins cannot delete other admins");
        }

        userRepository.delete(target);
    }

    @Transactional
    public UserResponse updateUserRole(User actor, Long targetId, Role newRole) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetId));

        // Only ADMINs can modify roles
        if (actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can modify user roles");
        }

        // Protected admin cannot be demoted
        if (isProtectedAdmin(target) && newRole != Role.ADMIN) {
            throw new ForbiddenException("Cannot demote the protected admin");
        }

        // Normal admin cannot modify other admins' roles
        if (!isProtectedAdmin(actor) && target.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Admins cannot modify other admins' roles");
        }

        target.setRole(newRole);
        User saved = userRepository.save(target);
        return convertToResponse(saved);
    }

    @Transactional
    public UserResponse promoteToAdmin(User actor, Long targetId) {
        return updateUserRole(actor, targetId, Role.ADMIN);
    }

    @Transactional
    public UserResponse demoteToUser(User actor, Long targetId) {
        return updateUserRole(actor, targetId, Role.USER);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToResponse(user);
    }

    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

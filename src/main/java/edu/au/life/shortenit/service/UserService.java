package edu.au.life.shortenit.service;

import edu.au.life.shortenit.dto.UserResponse;
import edu.au.life.shortenit.dto.UserUpdateRequest;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.exception.ResourceNotFoundException;
import edu.au.life.shortenit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public UserResponse getUserProfile(User user) {
        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateUserProfile(User user, UserUpdateRequest request) {
        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
            user = userRepository.save(user);
        }

        return convertToResponse(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
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

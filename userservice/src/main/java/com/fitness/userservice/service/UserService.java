package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.exception.InvalidUserDataException;
import com.fitness.userservice.exception.UserNotFoundException;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new InvalidUserDataException("Email cannot be empty");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new InvalidUserDataException("Password must be at least 6 characters");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new InvalidUserDataException("First name cannot be empty");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            User existingUser = userRepository.findByEmail(request.getEmail());

            if (request.getKeycloakId() != null && !request.getKeycloakId().equals(existingUser.getKeycloakId())) {
                log.info("Keycloak ID changed for {}. Updating database from {} to {}",
                        request.getEmail(), existingUser.getKeycloakId(), request.getKeycloakId());

                existingUser.setKeycloakId(request.getKeycloakId());
                // Save the updated ID back to the database
                userRepository.save(existingUser);
            } else {
                log.warn("User with email {} already exists", request.getEmail());
            }
            return getUserResponse(existingUser);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setKeycloakId(request.getKeycloakId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        try {
            // Attempt to save the user
            User savedUser = userRepository.save(user);
            log.info("User registered successfully with email: {}", savedUser.getEmail());
            return getUserResponse(savedUser);

        } catch (DataIntegrityViolationException e) {
            // If we hit this block, another thread beat us to the insert.
            // The user exists now, so we just fetch them and return success.
            log.info("Concurrent registration detected for email: {}. Retrieving existing user.", request.getEmail());
            User existingUser = userRepository.findByEmail(request.getEmail());
            return getUserResponse(existingUser);
        }
    }

    public UserResponse getUserProfile(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId)); // Since Frontend sends Keycloak ID as UserId

        return getUserResponse(user);
    }

    public Boolean existByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidUserDataException("User ID cannot be empty");
        }
        log.info("Validating user existence for userId: {}", userId);
        return userRepository.existsByKeycloakId(userId); // Use when frontend is finished and passes keycloakId
//        return userRepository.existsById(userId); // Use when Developing and passes userId
    }

    public UserResponse getUserResponse(User user) {
        UserResponse userResponse = new UserResponse();

        userResponse.setId(user.getId());
        userResponse.setKeycloakId(user.getKeycloakId());
        userResponse.setEmail(user.getEmail());
//        userResponse.setPassword(user.getPassword()); // Don't send passwords to frontend
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        return userResponse;
    }
}

package com.fitness.userservice.controller;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user registration, validation, and profile management")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile", description = "Retrieves user profile by Keycloak ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable("userId") String userId) {
        log.info("Getting user profile for id: {}", userId);
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with email, password, and profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully or already exists"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());
        return ResponseEntity.ok(userService.register(request));
    }

    @GetMapping("/{userId}/validate")
    @Operation(summary = "Validate user existence", description = "Checks if a user exists by Keycloak ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation result returned")
    })
    public ResponseEntity<Boolean> validateUser(@PathVariable("userId") String userId) {
        log.info("Validating user existence for userId: {}", userId);
        return ResponseEntity.ok(userService.existByUserId(userId));
    }

}

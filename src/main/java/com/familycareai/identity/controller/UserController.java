package com.familycareai.identity.controller;

import com.familycareai.common.dto.ApiResponse;
import com.familycareai.common.exception.UnauthorizedException;
import com.familycareai.common.util.SecurityUtils;
import com.familycareai.identity.dto.response.UserResponse;
import com.familycareai.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Identity Management", description = "Endpoints for managing authenticated user identity and profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get profile details of currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user found in SecurityContext"));

        UserResponse userResponse = userService.getUserProfileById(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User profile retrieved successfully"));
    }
}

package com.familycareai.identity.controller;

import com.familycareai.common.dto.ApiResponse;
import com.familycareai.identity.dto.request.LoginRequest;
import com.familycareai.identity.dto.request.LogoutRequest;
import com.familycareai.identity.dto.request.RefreshTokenRequest;
import com.familycareai.identity.dto.request.RegisterRequest;
import com.familycareai.identity.dto.response.AuthResponse;
import com.familycareai.identity.dto.response.TokenRefreshResponse;
import com.familycareai.identity.dto.response.UserResponse;
import com.familycareai.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity Management", description = "Endpoints for user registration, authentication, token rotation, and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = extractIpAddress(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        UserResponse response = authService.register(request, ipAddress, userAgent);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = extractIpAddress(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new Access/Refresh token pair")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        String ipAddress = extractIpAddress(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        TokenRefreshResponse response = authService.refresh(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String ipAddress = extractIpAddress(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        String userEmail = authentication != null ? authentication.getName() : null;

        authService.logout(request, userEmail, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

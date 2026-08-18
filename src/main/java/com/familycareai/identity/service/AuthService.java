package com.familycareai.identity.service;

import com.familycareai.audit.entity.AuditAction;
import com.familycareai.audit.entity.AuditStatus;
import com.familycareai.audit.service.AuditLogService;
import com.familycareai.common.exception.*;
import com.familycareai.common.util.DateUtils;
import com.familycareai.identity.dto.request.LoginRequest;
import com.familycareai.identity.dto.request.LogoutRequest;
import com.familycareai.identity.dto.request.RefreshTokenRequest;
import com.familycareai.identity.dto.request.RegisterRequest;
import com.familycareai.identity.dto.response.AuthResponse;
import com.familycareai.identity.dto.response.TokenRefreshResponse;
import com.familycareai.identity.dto.response.UserResponse;
import com.familycareai.identity.entity.*;
import com.familycareai.identity.event.UserRegisteredEvent;
import com.familycareai.identity.mapper.UserMapper;
import com.familycareai.identity.repository.RoleRepository;
import com.familycareai.identity.repository.UserRepository;
import com.familycareai.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoderService passwordEncoderService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("An account with email " + request.getEmail() + " already exists.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("An account with phone number " + request.getPhoneNumber() + " already exists.");
        }

        if (!DateUtils.isAdult(request.getDateOfBirth())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Account holder must be at least 18 years old.");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        // Medical professional roles require verification before activation
        AccountStatus initialStatus = isMedicalProfessionalRole(request.getRole())
                ? AccountStatus.PENDING_VERIFICATION
                : AccountStatus.ACTIVE;

        User user = userMapper.registerRequestToUser(request);
        user.setPasswordHash(passwordEncoderService.encode(request.getPassword()));
        user.setAccountStatus(initialStatus);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);
        user.setRoles(Set.of(role));

        User savedUser = userRepository.save(user);

        auditLogService.logAction(
                savedUser.getId(),
                AuditAction.USER_REGISTERED,
                ipAddress,
                userAgent,
                AuditStatus.SUCCESS,
                Map.of("email", savedUser.getEmail(), "role", request.getRole().name(), "status", initialStatus.name())
        );

        // Publish UserRegisteredEvent for asynchronous email dispatch
        eventPublisher.publishEvent(new UserRegisteredEvent(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                request.getRole().name()
        ));

        log.info("Successfully registered user {} with role {} and status {}", savedUser.getEmail(), request.getRole(), initialStatus);
        return userMapper.userToUserResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> {
                    auditLogService.logAction(
                            null,
                            AuditAction.LOGIN_FAILED,
                            ipAddress,
                            userAgent,
                            AuditStatus.FAILURE,
                            Map.of("email", request.getEmail(), "reason", "User not found")
                    );
                    return new BadCredentialsException("Invalid email or password");
                });

        // Enforce Brute-Force Account Lockout
        if (!user.isAccountNonLocked()) {
            long minutesRemaining = Duration.between(Instant.now(), user.getLockoutUntil()).toMinutes() + 1;
            auditLogService.logAction(
                    user.getId(),
                    AuditAction.LOGIN_FAILED,
                    ipAddress,
                    userAgent,
                    AuditStatus.FAILURE,
                    Map.of("reason", "Attempted login on locked account", "lockoutMinutesRemaining", minutesRemaining)
            );
            throw new AccountLockedException("Account is locked due to multiple failed login attempts. Please try again after " + minutesRemaining + " minutes.");
        }

        // Lock expired -> reset lockout state
        if (user.getLockoutUntil() != null && Instant.now().isAfter(user.getLockoutUntil())) {
            user.setLockoutUntil(null);
            user.setFailedLoginAttempts(0);
        }

        // Validate Password
        if (!passwordEncoderService.matches(request.getPassword(), user.getPasswordHash())) {
            int newFailedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(newFailedAttempts);

            if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockoutUntil(Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES));
                userRepository.save(user);

                auditLogService.logAction(
                        user.getId(),
                        AuditAction.LOGIN_FAILED,
                        ipAddress,
                        userAgent,
                        AuditStatus.FAILURE,
                        Map.of("reason", "Maximum failed login attempts reached - Account locked for 15 minutes", "attempts", newFailedAttempts)
                );

                throw new AccountLockedException("Account is temporarily locked due to 5 consecutive failed login attempts. Try again in 15 minutes.");
            }

            userRepository.save(user);

            auditLogService.logAction(
                    user.getId(),
                    AuditAction.LOGIN_FAILED,
                    ipAddress,
                    userAgent,
                    AuditStatus.FAILURE,
                    Map.of("reason", "Invalid password", "failedAttempts", newFailedAttempts)
            );

            throw new BadCredentialsException("Invalid email or password");
        }

        // Successful authentication -> Reset lockout counters & update last login
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String refreshToken = tokenService.createRefreshToken(user, request.getDeviceInfo(), ipAddress);

        auditLogService.logAction(
                user.getId(),
                AuditAction.LOGIN_SUCCESS,
                ipAddress,
                userAgent,
                AuditStatus.SUCCESS,
                Map.of("deviceInfo", request.getDeviceInfo() != null ? request.getDeviceInfo() : "unknown")
        );

        log.info("User {} logged in successfully", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenProvider.getExpirationMinutes() * 60)
                .user(userMapper.userToUserResponse(user))
                .build();
    }

    @Transactional
    public TokenRefreshResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        TokenService.TokenPairResult result = tokenService.verifyAndRotateRefreshToken(
                request.getRefreshToken(), ipAddress, userAgent
        );

        User user = result.getUser();
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleNames);

        auditLogService.logAction(
                user.getId(),
                AuditAction.TOKEN_REFRESHED,
                ipAddress,
                userAgent,
                AuditStatus.SUCCESS,
                Map.of("email", user.getEmail())
        );

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(result.getNewRawRefreshToken())
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenProvider.getExpirationMinutes() * 60)
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request, String currentUserEmail, String ipAddress, String userAgent) {
        tokenService.revokeToken(request.getRefreshToken());

        User actorUser = null;
        if (currentUserEmail != null) {
            actorUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        }

        auditLogService.logAction(
                actorUser != null ? actorUser.getId() : null,
                AuditAction.LOGOUT,
                ipAddress,
                userAgent,
                AuditStatus.SUCCESS,
                Map.of("email", currentUserEmail != null ? currentUserEmail : "anonymous")
        );

        log.info("Logged out session for user email: {}", currentUserEmail);
    }

    private boolean isMedicalProfessionalRole(RoleName roleName) {
        return roleName == RoleName.ROLE_DOCTOR
                || roleName == RoleName.ROLE_PHARMACIST
                || roleName == RoleName.ROLE_LAB_TECHNICIAN;
    }
}

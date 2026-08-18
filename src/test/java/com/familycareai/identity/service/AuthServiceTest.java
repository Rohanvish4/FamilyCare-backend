package com.familycareai.identity.service;

import com.familycareai.audit.service.AuditLogService;
import com.familycareai.common.exception.AccountLockedException;
import com.familycareai.common.exception.UserAlreadyExistsException;
import com.familycareai.identity.dto.request.LoginRequest;
import com.familycareai.identity.dto.request.RegisterRequest;
import com.familycareai.identity.dto.response.AuthResponse;
import com.familycareai.identity.dto.response.UserResponse;
import com.familycareai.identity.entity.*;
import com.familycareai.identity.event.UserRegisteredEvent;
import com.familycareai.identity.mapper.UserMapper;
import com.familycareai.identity.repository.RoleRepository;
import com.familycareai.identity.repository.UserRepository;
import com.familycareai.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoderService passwordEncoderService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private Role patientRole;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("jane.doe@familycare.ai")
                .password("SecurePass123!")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("+919876543210")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.FEMALE)
                .role(RoleName.ROLE_PATIENT)
                .build();

        patientRole = Role.builder()
                .id((short) 1)
                .name(RoleName.ROLE_PATIENT)
                .description("Patient role")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .email("jane.doe@familycare.ai")
                .passwordHash("hashed_password")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("+919876543210")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.FEMALE)
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .roles(Set.of(patientRole))
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new patient account and publish UserRegisteredEvent")
    void testRegisterPatientSuccess() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_PATIENT)).thenReturn(Optional.of(patientRole));
        when(userMapper.registerRequestToUser(registerRequest)).thenReturn(user);
        when(passwordEncoderService.encode(registerRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse mockResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .accountStatus(AccountStatus.ACTIVE)
                .roles(Set.of("ROLE_PATIENT"))
                .build();
        when(userMapper.userToUserResponse(user)).thenReturn(mockResponse);

        UserResponse result = authService.register(registerRequest, "127.0.0.1", "Test-Agent");

        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(AccountStatus.ACTIVE, result.getAccountStatus());
        verify(userRepository, times(1)).save(any(User.class));
        verify(eventPublisher, times(1)).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when registering with duplicate email and not publish event")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                authService.register(registerRequest, "127.0.0.1", "Test-Agent")
        );

        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest("jane.doe@familycare.ai", "SecurePass123!", "Web");

        when(userRepository.findByEmailWithRoles(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoderService.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock_jwt_access_token");
        when(tokenService.createRefreshToken(any(), any(), any())).thenReturn("mock_raw_refresh_token");
        when(jwtTokenProvider.getExpirationMinutes()).thenReturn(15L);

        UserResponse mockResponse = UserResponse.builder().id(user.getId()).email(user.getEmail()).build();
        when(userMapper.userToUserResponse(user)).thenReturn(mockResponse);

        AuthResponse authResponse = authService.login(loginRequest, "127.0.0.1", "Web");

        assertNotNull(authResponse);
        assertEquals("mock_jwt_access_token", authResponse.getAccessToken());
        assertEquals("mock_raw_refresh_token", authResponse.getRefreshToken());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should increment failed attempts on invalid password and lock after 5 attempts")
    void testLoginBruteForceLockout() {
        LoginRequest loginRequest = new LoginRequest("jane.doe@familycare.ai", "WrongPassword", "Web");
        user.setFailedLoginAttempts(4);

        when(userRepository.findByEmailWithRoles(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoderService.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(false);

        assertThrows(AccountLockedException.class, () ->
                authService.login(loginRequest, "127.0.0.1", "Web")
        );

        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockoutUntil());
        verify(userRepository, times(1)).save(user);
    }
}

package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.User;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.UserRole;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.AuthResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.LoginRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.RegisterRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.dto.UserDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded-password")
                .phone("+123456789")
                .role(UserRole.STUDENT)
                .isEmailVerified(true)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    // =========================
    // loadUserByUsername
    // =========================
    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetails() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = authenticationService.loadUserByUsername("john@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("john@example.com");
        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    void loadUserByUsername_whenUserMissing_throwsException() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email");

        verify(userRepository).findByEmail("missing@example.com");
    }

    // =========================
    // register
    // =========================
    @Test
    void register_whenEmailNotExists_createsUserAndReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("John@Example.com");
        request.setPassword("plain-password");
        request.setPhone("+123456789");
        request.setRole(UserRole.STUDENT.name());

        when(userRepository.existsByEmailIgnoreCase("John@Example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        // Build the saved user explicitly (no toBuilder)
        User savedUser = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com") // lowercased
                .password("encoded-password")
                .phone("+123456789")
                .role(UserRole.STUDENT)
                .isEmailVerified(true)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthResponse response = authenticationService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(savedUser.getId());
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.STUDENT.name());

        verify(userRepository).existsByEmailIgnoreCase("John@Example.com");
        verify(passwordEncoder).encode("plain-password");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(savedUser);
    }

    @Test
    void register_whenEmailExists_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository).existsByEmailIgnoreCase("john@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    // =========================
    // login
    // =========================
    @Test
    void login_whenCredentialsValid_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("plain-password");
        String ipAddress = "127.0.0.1";

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password"))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authenticationService.login(request, ipAddress);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.STUDENT.name());

        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).matches("plain-password", "encoded-password");
        verify(jwtService).generateToken(user);
        verify(userRepository, atLeastOnce()).save(user); // for recordSuccessfulLogin
    }

    @Test
    void login_whenUserNotFound_throwsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("any");

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail("missing@example.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_whenAccountLocked_throwsBadCredentials() {
        // Build a locked user explicitly (no toBuilder)
        User lockedUser = User.builder()
                .id(userId)
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .phone(user.getPhone())
                .role(user.getRole())
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(user.getIsActive())
                .isLocked(true)
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("any");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Account is locked");

        verify(userRepository).findByEmail("john@example.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_whenAccountInactive_throwsBadCredentials() {
        // Build an inactive user explicitly (no toBuilder)
        User inactiveUser = User.builder()
                .id(userId)
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .phone(user.getPhone())
                .role(user.getRole())
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(false)
                .isLocked(user.getIsLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("any");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Account is inactive");

        verify(userRepository).findByEmail("john@example.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_whenPasswordInvalid_incrementsFailedLoginAndThrowsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verify(userRepository).save(user); // for recordFailedLogin
        verifyNoInteractions(jwtService);
    }

    // =========================
    // getUserById / getUserByEmail
    // =========================
    @Test
    void getUserById_whenUserExists_returnsUserDTO() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDTO dto = authenticationService.getUserById(userId);

        assertThat(dto.getId()).isEqualTo(userId);
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getRole()).isEqualTo(UserRole.STUDENT.name());
        assertThat(dto.getIsActive()).isTrue();

        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_whenUserMissing_throwsResourceNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("id");

        verify(userRepository).findById(userId);
    }

    @Test
    void getUserByEmail_whenUserExists_returnsUserDTO() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        UserDTO dto = authenticationService.getUserByEmail("john@example.com");

        assertThat(dto.getId()).isEqualTo(userId);
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getRole()).isEqualTo(UserRole.STUDENT.name());

        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    void getUserByEmail_whenUserMissing_throwsResourceNotFound() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.getUserByEmail("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("email");

        verify(userRepository).findByEmail("missing@example.com");
    }
}
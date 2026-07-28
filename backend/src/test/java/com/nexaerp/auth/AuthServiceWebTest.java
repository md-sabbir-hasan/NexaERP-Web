package com.nexaerp.auth;

import com.nexaerp.audit.AuditLogService;
import com.nexaerp.auth.dto.RefreshTokenRequestDto;
import com.nexaerp.common.exception.BrowserAuthenticationException;
import com.nexaerp.email.EmailService;
import com.nexaerp.passwordreset.PasswordResetTokenRepository;
import com.nexaerp.security.JwtUtil;
import com.nexaerp.token.RefreshToken;
import com.nexaerp.token.RefreshTokenRepository;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.verification.EmailVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceWebTest {
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuditLogService auditLogService;
    @Mock EmailVerificationRepository emailVerificationRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock EmailService emailService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, refreshTokenRepository, passwordEncoder,
                jwtUtil, auditLogService, emailVerificationRepository,
                passwordResetTokenRepository, emailService);
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", 900000L);
    }

    @Test
    void webRefreshRevokesOldTokenAndCreatesReplacement() {
        User user = activeUser();
        RefreshToken current = RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("old")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(current));
        when(jwtUtil.generateAccessToken(7L, user.getEmail(), java.util.List.of()))
                .thenReturn("access");

        BrowserAuthResult result = service.webRefresh("old", "127.0.0.1", "browser");

        assertThat(current.getRevoked()).isTrue();
        assertThat(result.response().getAccessToken()).isEqualTo("access");
        assertThat(result.response()).hasNoNullFieldsOrPropertiesExcept();
        assertThat(result.refreshToken()).isNotEqualTo("old");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getToken()).isEqualTo(result.refreshToken());
        assertThat(captor.getAllValues().get(1).getRevoked()).isFalse();
    }

    @Test
    void webRefreshRejectsInactiveUserBeforeCreatingReplacement() {
        User user = activeUser();
        user.setStatus(UserStatus.INACTIVE);
        RefreshToken current = RefreshToken.builder()
                .user(user)
                .token("old")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.webRefresh("old", "127.0.0.1", "browser"))
                .isInstanceOf(BrowserAuthenticationException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void oldBrowserTokenFailsAfterRotation() {
        RefreshToken revoked = RefreshToken.builder()
                .user(activeUser())
                .token("old")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenForUpdate("old")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.webRefresh("old", "127.0.0.1", "browser"))
                .isInstanceOf(BrowserAuthenticationException.class);
    }

    @Test
    void mobileRefreshStillReturnsSameJsonRefreshToken() {
        User user = activeUser();
        RefreshToken current = RefreshToken.builder()
                .user(user)
                .token("mobile")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByToken("mobile")).thenReturn(Optional.of(current));
        when(jwtUtil.generateAccessToken(7L, user.getEmail(), java.util.List.of()))
                .thenReturn("access");

        var response = service.refresh(new RefreshTokenRequestDto("mobile"));

        assertThat(response.getRefreshToken()).isEqualTo("mobile");
    }

    private User activeUser() {
        return User.builder()
                .id(7L)
                .name("Web User")
                .email("web@nexaerp.test")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();
    }
}

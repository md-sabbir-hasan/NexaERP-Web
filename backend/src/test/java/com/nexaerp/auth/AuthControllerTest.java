package com.nexaerp.auth;

import com.nexaerp.security.CurrentUserPrincipal;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthService authService;
    @Mock UserRepository userRepository;

    @Test
    void meUsesEmailFromJwtPrincipal() {
        AuthController controller = new AuthController(authService, userRepository);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(42L, "user@nexaerp.test");
        User user = User.builder()
                .id(42L)
                .name("Dashboard User")
                .email(principal.email())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();
        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(user));

        var response = controller.me(principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(principal.email());
        verify(userRepository).findByEmail(principal.email());
    }

    @Test
    void mobileLogoutUsesCurrentUserPrincipalAndKeepsLogoutAllContract() {
        AuthController controller = new AuthController(authService, userRepository);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(42L, "user@nexaerp.test");
        User user = User.builder().id(42L).email(principal.email()).build();
        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(user));

        controller.logout(principal);

        verify(authService).logout(42L);
    }
}

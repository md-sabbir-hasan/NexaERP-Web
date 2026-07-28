package com.nexaerp.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieServiceTest {
    private final RefreshTokenCookieService service =
            new RefreshTokenCookieService(
                    "nexa_refresh_token", false, "Strict", "/api/auth/web", 604800);

    @Test
    void createsHttpOnlyCookieWithConfiguredAttributes() {
        String header = service.create("opaque-value").toString();

        assertThat(header)
                .contains("nexa_refresh_token=opaque-value")
                .contains("Path=/api/auth/web")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .doesNotContain("Secure");
    }

    @Test
    void expirationUsesMatchingNameAndPath() {
        String header = service.expire().toString();

        assertThat(header)
                .contains("nexa_refresh_token=")
                .contains("Path=/api/auth/web")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("SameSite=Strict");
    }

    @Test
    void readsOnlyConfiguredCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "ignored"),
                new Cookie("nexa_refresh_token", "expected"));

        assertThat(service.read(request)).contains("expected");
    }
}

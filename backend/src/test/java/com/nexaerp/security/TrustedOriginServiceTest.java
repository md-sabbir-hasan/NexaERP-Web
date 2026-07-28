package com.nexaerp.security;

import com.nexaerp.common.exception.BrowserAuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrustedOriginServiceTest {
    private final TrustedOriginService service =
            new TrustedOriginService("http://localhost:4200,https://erp.example.com");

    @Test
    void acceptsConfiguredExactOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://localhost:4200");

        assertThatCode(() -> service.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUntrustedOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://attacker.example");

        assertThatThrownBy(() -> service.validate(request))
                .isInstanceOf(BrowserAuthenticationException.class);
    }

    @Test
    void rejectsCrossSiteBrowserRequestWithoutOriginMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Sec-Fetch-Site", "cross-site");

        assertThatThrownBy(() -> service.validate(request))
                .isInstanceOf(BrowserAuthenticationException.class);
    }

    @Test
    void allowsExplicitNonBrowserRequestWithoutBrowserOriginMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatCode(() -> service.validate(request)).doesNotThrowAnyException();
    }
}

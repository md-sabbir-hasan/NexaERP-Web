package com.nexaerp.security;

import com.nexaerp.common.exception.BrowserAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrustedOriginService {
    private final Set<String> trustedOrigins;

    public TrustedOriginService(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.trustedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validate(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            requireTrusted(origin);
            return;
        }

        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                requireTrusted(uri.getScheme() + "://" + uri.getAuthority());
                return;
            } catch (IllegalArgumentException exception) {
                throw new BrowserAuthenticationException("Untrusted request origin");
            }
        }

        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if ("cross-site".equalsIgnoreCase(fetchSite)) {
            throw new BrowserAuthenticationException("Untrusted request origin");
        }
        // Requests without browser origin metadata are allowed for same-origin and non-browser clients.
    }

    private void requireTrusted(String origin) {
        if (!trustedOrigins.contains(origin)) {
            throw new BrowserAuthenticationException("Untrusted request origin");
        }
    }
}

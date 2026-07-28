package com.nexaerp.auth;

import com.nexaerp.auth.dto.LoginRequestDto;
import com.nexaerp.auth.dto.WebAuthResponseDto;
import com.nexaerp.common.exception.BrowserAuthenticationException;
import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.security.TrustedOriginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/web")
@RequiredArgsConstructor
public class WebAuthController {
    private final AuthService authService;
    private final RefreshTokenCookieService cookieService;
    private final TrustedOriginService trustedOriginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<WebAuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        trustedOriginService.validate(httpRequest);
        BrowserAuthResult result = authService.webLogin(
                request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(result.refreshToken()).toString())
                .body(ApiResponse.success("Login successful", result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<WebAuthResponseDto>> refresh(HttpServletRequest request) {
        trustedOriginService.validate(request);
        String refreshToken = cookieService.read(request)
                .orElseThrow(() -> new BrowserAuthenticationException("Refresh session is missing"));
        BrowserAuthResult result = authService.webRefresh(
                refreshToken, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(result.refreshToken()).toString())
                .body(ApiResponse.success("Token refreshed", result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        trustedOriginService.validate(request);
        cookieService.read(request).ifPresent(authService::webLogout);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.expire().toString())
                .body(ApiResponse.success("Logged out successfully", null));
    }
}

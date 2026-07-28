package com.nexaerp.auth;

import com.nexaerp.auth.dto.WebAuthResponseDto;

public record BrowserAuthResult(
        WebAuthResponseDto response,
        String refreshToken
) {
}

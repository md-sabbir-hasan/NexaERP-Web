package com.nexaerp.token;

import com.nexaerp.user.User;

public interface RefreshTokenService {

    // Create and save a new refresh token for user
    RefreshToken createRefreshToken(User user, String ipAddress, String deviceName);

    // Find token by value
    RefreshToken findByToken(String token);

    // Validate token — check revoked and expiry
    void validateRefreshToken(RefreshToken token);

    // Revoke a single token
    void revokeToken(String token);

    // Revoke all tokens for a user (logout from all devices)
    void revokeAllUserTokens(Long userId);
}
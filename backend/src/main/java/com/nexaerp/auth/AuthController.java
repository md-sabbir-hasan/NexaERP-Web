package com.nexaerp.auth;


import com.nexaerp.auth.dto.*;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.role.Role;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;
    private final UserRepository userRepository;


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String deviceName = httpRequest.getHeader("User-Agent");

        return ResponseEntity.ok(ApiResponse.success("Login successful",
                authService.login(request, ipAddress, deviceName)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed",
                authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        authService.logout(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    // Current logged-in user's profile + roles + permissions.
    // Any authenticated user can call this for themselves — no special permission required.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponseDto>> me(
            @AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        CurrentUserResponseDto dto = CurrentUserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .roles(roleNames)
                .permissions(permissions)
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

//    ===============email========

    // Verify email
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    // Resend verification
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }

    // Forgot password
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset email sent", null));
    }

    // Reset password
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successfully", null));
    }


    // Validate invite token (Frontend এ Set Password page load হলে call করবে)
    @GetMapping("/validate-invite")
    public ResponseEntity<ApiResponse<Void>> validateInvite(
            @RequestParam String token) {
        authService.validateInviteToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token is valid", null));
    }

    // Set password using invite token
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordRequestDto request) {
        authService.setPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password set successfully. You can now login.", null));
    }

    // Resend invite
    @PostMapping("/resend-invite")
    public ResponseEntity<ApiResponse<Void>> resendInvite(
            @RequestParam String email) {
        authService.resendInvite(email);
        return ResponseEntity.ok(ApiResponse.success("Invite email resent", null));
    }
}

package com.nexaerp.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;      // access token expiry in ms
    private Long userId;
    private String name;
    private String email;
}

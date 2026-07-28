package com.nexaerp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebAuthResponseDto {
    private String accessToken;
    private Long expiresIn;
    private Long userId;
    private String name;
    private String email;
}

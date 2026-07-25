package com.nexaerp.permission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponseDto {
    private Long id;
    private String code;
    private String name;
    private String module;
}

package com.nexaerp.role.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDto {
    private Long id;
    private String name;
    private String description;
    private Set<PermissionDto> permissions;
    private Integer userCount;



    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionDto {
        private Long id;
        private String code;
        private String name;
        private String module;
    }
}

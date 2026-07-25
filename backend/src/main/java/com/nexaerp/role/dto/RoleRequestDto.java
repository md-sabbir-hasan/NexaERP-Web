package com.nexaerp.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDto {
    @NotBlank(message = "Role name is required")
    private String name;

    private String description;

    // Permission IDs to assign
    private Set<Long> permissionIds;
}

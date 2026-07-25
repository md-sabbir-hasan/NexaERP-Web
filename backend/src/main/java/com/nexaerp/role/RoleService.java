package com.nexaerp.role;

import com.nexaerp.role.dto.RoleRequestDto;
import com.nexaerp.role.dto.RoleResponseDto;

import java.util.List;

public interface RoleService {
    // Get all roles with their permissions
    List<RoleResponseDto> getAll();

    // Get single role by ID
    RoleResponseDto getById(Long id);

    // Create new role
    RoleResponseDto create(RoleRequestDto request);

    // Update role name, description, permissions
    RoleResponseDto update(Long id, RoleRequestDto request);

    // Assign permissions to role
    RoleResponseDto assignPermissions(Long id, java.util.Set<Long> permissionIds);

    // Remove permissions from role
    RoleResponseDto removePermissions(Long id, java.util.Set<Long> permissionIds);
}

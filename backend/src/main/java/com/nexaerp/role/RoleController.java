package com.nexaerp.role;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.role.dto.RoleRequestDto;
import com.nexaerp.role.dto.RoleResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;


    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAll()));
    }

    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getById(id)));
    }

    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponseDto>> create(
            @Valid @RequestBody RoleRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Role created",
                roleService.create(request)));
    }

    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Role updated",
                roleService.update(id, request)));
    }

    // Assign permissions to role
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @PostMapping("/{id}/permissions/assign")
    public ResponseEntity<ApiResponse<RoleResponseDto>> assignPermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(ApiResponse.success("Permissions assigned",
                roleService.assignPermissions(id, permissionIds)));
    }

    // Remove permissions from role
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    @PostMapping("/{id}/permissions/remove")
    public ResponseEntity<ApiResponse<RoleResponseDto>> removePermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(ApiResponse.success("Permissions removed",
                roleService.removePermissions(id, permissionIds)));
    }
}

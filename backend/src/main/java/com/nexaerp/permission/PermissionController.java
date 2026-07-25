package com.nexaerp.permission;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.permission.dto.PermissionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;


    // Get all permissions (grouped by module)
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponseDto>>> getAll() {
        List<PermissionResponseDto> permissions = permissionRepository.findAll()
                .stream()
                .map(p -> PermissionResponseDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .module(p.getModule())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    // Get permissions by module
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    @GetMapping("/module/{module}")
    public ResponseEntity<ApiResponse<List<PermissionResponseDto>>> getByModule(
            @PathVariable String module) {
        List<PermissionResponseDto> permissions = permissionRepository.findByModule(module)
                .stream()
                .map(p -> PermissionResponseDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .module(p.getModule())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }
}

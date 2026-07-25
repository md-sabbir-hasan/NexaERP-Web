package com.nexaerp.role;


import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.permission.Permission;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.role.dto.RoleRequestDto;
import com.nexaerp.role.dto.RoleResponseDto;
import com.nexaerp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService{

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;


    @Override
    public List<RoleResponseDto> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoleResponseDto getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponseDto create(RoleRequestDto request) {
        // Name unique check
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessRuleException("Role name already exists: " + request.getName());
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null) {
            permissions = getPermissions(request.getPermissionIds());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(permissions)
                .build();

        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponseDto update(Long id, RoleRequestDto request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));


        if (role.getName().equalsIgnoreCase("SUPER_ADMIN")) {
            throw new BusinessRuleException("SUPER_ADMIN role cannot be updated");
        }

        // Name change then -- unique check
        if (!role.getName().equals(request.getName()) &&
                roleRepository.existsByName(request.getName())) {
            throw new BusinessRuleException("Role name already exists: " + request.getName());
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissionIds() != null) {
            role.setPermissions(getPermissions(request.getPermissionIds()));
        }

        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponseDto assignPermissions(Long id, Set<Long> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getName().equalsIgnoreCase("SUPER_ADMIN")) {
            throw new BusinessRuleException("SUPER_ADMIN permissions cannot be modified");
        }

        // Add new permissions to existing ones
        Set<Permission> newPermissions = getPermissions(permissionIds);
        role.getPermissions().addAll(newPermissions);

        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponseDto removePermissions(Long id, Set<Long> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getName().equalsIgnoreCase("SUPER_ADMIN")) {
            throw new BusinessRuleException("SUPER_ADMIN permissions cannot be modified");
        }


        // Remove specified permissions
        role.getPermissions().removeIf(p -> permissionIds.contains(p.getId()));

        return toResponse(roleRepository.save(role));
    }




    //------private--- Helper--------

    private Set<Permission> getPermissions(Set<Long> permissionIds) {
        Set<Permission> permissions = new HashSet<>();
        for (Long permId : permissionIds) {
            Permission permission = permissionRepository.findById(permId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Permission not found: " + permId));
            permissions.add(permission);
        }
        return permissions;
    }

    private RoleResponseDto toResponse(Role role) {

        // Count users with this role
//        long userCount = userRepository.findAll()
//                .stream()
//                .filter(u -> u.getRoles().stream()
//                        .anyMatch(r -> r.getId().equals(role.getId())))
//                .count();

        long userCount = userRepository.countByRoles_Id(role.getId());

        Set<RoleResponseDto.PermissionDto> permissionDtos = role.getPermissions()
                .stream()
                .map(p -> RoleResponseDto.PermissionDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .module(p.getModule())
                        .build())
                .collect(Collectors.toSet());

        return RoleResponseDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionDtos)
                .userCount((int) userCount)
                .build();
    }
}

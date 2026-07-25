package com.nexaerp.user;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.auth.AuthService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.role.Role;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.dto.UserRequestDto;
import com.nexaerp.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexaerp.common.response.PageResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final AuthService authService;


    @Override
    @Transactional
    public UserResponseDto create(UserRequestDto request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + request.getEmail());
        }

        Set<Role> roles = getRoles(request.getRoleIds());
        preventSuperAdminRoleAssignment(roles, null);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(null)           // No password yet
                .status(UserStatus.PENDING) // PENDING until password set
                .failedLoginAttempts(0)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);

        // Send invite email
        authService.sendInviteEmail(saved);

        // Audit log
        auditLogService.log(AuditAction.CREATED, "USER", saved.getId(),
                null, saved.getEmail());

        return toResponse(saved);
    }

    @Override
    public UserResponseDto update(Long id, UserRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Email change--- unique check
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + request.getEmail());
        }

        Set<Role> roles = getRoles(request.getRoleIds());

        preventSuperAdminUserUpdate(user);
        preventSuperAdminRoleAssignment(roles, user);

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRoles(getRoles(request.getRoleIds()));

        User updated = userRepository.save(user);

        // Audit
        auditLogService.log(
                AuditAction.UPDATED,
                "USER",
                updated.getId(),
                null,
                updated.getEmail()
        );

        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponseDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Override
    public List<UserResponseDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());


    }


    @Override
    public PageResponseDto<UserResponseDto> getAllPaged(
            int page,
            int size,
            String search,
            UserStatus status
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        boolean hasSearch = search != null && !search.trim().isEmpty();

        Page<User> users;

        if (hasSearch && status != null) {
            String keyword = search.trim();

            users = userRepository
                    .findByStatusAndNameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCase(
                            status,
                            keyword,
                            status,
                            keyword,
                            pageable
                    );
        } else if (hasSearch) {
            String keyword = search.trim();

            users = userRepository
                    .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            keyword,
                            keyword,
                            pageable
                    );
        } else if (status != null) {
            users = userRepository.findByStatus(status, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        Page<UserResponseDto> responsePage = users.map(this::toResponse);

        return PageResponseDto.from(responsePage);
    }

    @Override
    @Transactional
    public void deactivate(Long id, String currentUserEmail) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // self account can not deactivate
        if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new BusinessRuleException("You cannot deactivate your own account");
        }

        // Super Admin account can not deactivate
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("SUPER_ADMIN"));

        if (isSuperAdmin) {
            throw new BusinessRuleException("SUPER_ADMIN account cannot be deactivated");
        }

        // Already inactive check
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessRuleException("User is already inactive");
        }

        UserStatus oldStatus = user.getStatus();

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        auditLogService.log(
                AuditAction.DEACTIVATED,
                "USER",
                user.getId(),
                oldStatus.name(),
                "INACTIVE"
        );
    }

    @Override
    @Transactional
    public void activate(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserStatus oldStatus = user.getStatus();

        if (oldStatus == UserStatus.ACTIVE) {
            throw new BusinessRuleException("User is already active");
        }


        if (oldStatus == UserStatus.PENDING) {
            throw new BusinessRuleException(
                    "Pending user cannot be activated. The user must set a password using the invitation link."
            );
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessRuleException(
                    "User cannot be activated before setting a password."
            );
        }

        

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);

        auditLogService.log(
                AuditAction.ACTIVATED,
                "USER",
                user.getId(),
                oldStatus.name(),
                UserStatus.ACTIVE.name()
        );
    }


                                      // --Private Helper---++++##


    private Set<Role> getRoles(Set<Long> roleIds) {
        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
            roles.add(role);
        }
        return roles;
    }

    private UserResponseDto toResponse(User user) {

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(roleNames)
                .permissions(permissions)
                .build();
    }


    private void preventSuperAdminUserUpdate(User user) {
        boolean isSuperAdminUser = user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("SUPER_ADMIN"));

        if (isSuperAdminUser) {
            throw new BusinessRuleException("SUPER_ADMIN user cannot be updated");
        }
    }

    private void preventSuperAdminRoleAssignment(Set<Role> roles, User existingUser) {
        boolean hasSuperAdminRole = roles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("SUPER_ADMIN"));

        if (hasSuperAdminRole) {
            throw new BusinessRuleException("SUPER_ADMIN role cannot be assigned manually");
        }
    }

}

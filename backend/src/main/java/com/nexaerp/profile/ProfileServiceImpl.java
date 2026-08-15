package com.nexaerp.profile;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.auth.dto.CurrentUserResponseDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.fileupload.FileUploadService;
import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import com.nexaerp.profile.dto.ProfileUpdateRequestDto;
import com.nexaerp.role.Role;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private static final long MAX_PHOTO_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final String ENTITY_TYPE = "PROFILE";
    private static final String FILES_URL_PREFIX = "/api/files/";

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CurrentUserResponseDto updateName(
            String currentUserEmail,
            ProfileUpdateRequestDto request
    ) {
        User user = getUser(currentUserEmail);
        String oldName = user.getName();

        user.setName(request.getName().trim());
        User saved = userRepository.save(user);

        auditLogService.log(
                AuditAction.UPDATED,
                "USER",
                user.getId(),
                oldName,
                saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public CurrentUserResponseDto uploadPhoto(
            String currentUserEmail,
            MultipartFile file
    ) {
        validatePhoto(file);

        User user = getUser(currentUserEmail);
        String oldImageUrl = user.getProfileImageUrl();

        FileUploadResponseDto uploaded =
                fileUploadService.upload(file, ENTITY_TYPE, user.getId());

        user.setProfileImageUrl(uploaded.getFileUrl());
        User saved = userRepository.save(user);

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            fileUploadService.delete(toRelativePath(oldImageUrl));
        }

        auditLogService.log(
                AuditAction.UPDATED,
                "USER",
                user.getId(),
                oldImageUrl,
                saved.getProfileImageUrl()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public CurrentUserResponseDto removePhoto(String currentUserEmail) {
        User user = getUser(currentUserEmail);
        String oldImageUrl = user.getProfileImageUrl();

        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            throw new BusinessRuleException("No profile photo to remove");
        }

        user.setProfileImageUrl(null);
        User saved = userRepository.save(user);

        fileUploadService.delete(toRelativePath(oldImageUrl));

        auditLogService.log(
                AuditAction.UPDATED,
                "USER",
                user.getId(),
                oldImageUrl,
                null
        );

        return toResponse(saved);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Profile photo is required");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new BusinessRuleException("Profile photo must not exceed 2 MB");
        }
        if (!ALLOWED_PHOTO_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException("Only JPG and PNG images are allowed");
        }
    }

    private String toRelativePath(String fileUrl) {
        return fileUrl.startsWith(FILES_URL_PREFIX)
                ? fileUrl.substring(FILES_URL_PREFIX.length())
                : fileUrl;
    }

    private CurrentUserResponseDto toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        return CurrentUserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(roleNames)
                .permissions(permissions)
                .build();
    }
}
package com.nexaerp.profile;

import com.nexaerp.auth.dto.CurrentUserResponseDto;
import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.profile.dto.ProfileUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping
    public ResponseEntity<ApiResponse<CurrentUserResponseDto>> updateName(
            @Valid @RequestBody ProfileUpdateRequestDto request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated",
                profileService.updateName(authentication.getName(), request)
        ));
    }

    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CurrentUserResponseDto>> uploadPhoto(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile photo updated",
                profileService.uploadPhoto(authentication.getName(), file)
        ));
    }

    @DeleteMapping("/photo")
    public ResponseEntity<ApiResponse<CurrentUserResponseDto>> removePhoto(
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile photo removed",
                profileService.removePhoto(authentication.getName())
        ));
    }
}
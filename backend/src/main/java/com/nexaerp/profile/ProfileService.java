package com.nexaerp.profile;

import com.nexaerp.auth.dto.CurrentUserResponseDto;
import com.nexaerp.profile.dto.ProfileUpdateRequestDto;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    CurrentUserResponseDto updateName(
            String currentUserEmail,
            ProfileUpdateRequestDto request
    );

    CurrentUserResponseDto uploadPhoto(
            String currentUserEmail,
            MultipartFile file
    );

    CurrentUserResponseDto removePhoto(String currentUserEmail);
}
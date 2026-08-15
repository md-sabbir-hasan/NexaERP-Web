package com.nexaerp.user.profile;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageService {
    String upload(Long userId, MultipartFile file);

    void delete(String imageUrl);
}

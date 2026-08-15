package com.nexaerp.user.profile;

import com.nexaerp.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageServiceImpl implements ProfileImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public String upload(Long userId, MultipartFile file) {

        validate(file);

        try {
            Path directory = Paths.get(uploadDir, "profile");

            Files.createDirectories(directory);

            String extension = getExtension(file.getOriginalFilename());

            String fileName =
                    userId + "-" + UUID.randomUUID() + extension;

            Path target = directory.resolve(fileName);

            Files.copy(file.getInputStream(), target);

            return "/uploads/profile/" + fileName;

        } catch (IOException e) {
            throw new BusinessRuleException(
                    "Failed to upload profile image"
            );
        }
    }

    @Override
    public void delete(String imageUrl) {

        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String relativePath = imageUrl.replaceFirst("^/uploads/", "");

            Path file = Paths.get(uploadDir).resolve(relativePath);

            Files.deleteIfExists(file);

        } catch (IOException ignored) {
            // Don't fail profile update if old image cannot be deleted
        }
    }

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(
                    "Profile image is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException(
                    "Profile image must not exceed 2 MB"
            );
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessRuleException(
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }
    }

    private String getExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }

        return fileName.substring(
                fileName.lastIndexOf('.')
        ).toLowerCase();
    }
}
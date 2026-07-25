package com.nexaerp.fileupload;

import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.allowed-types}")
    private String allowedTypes;

    @Override
    public FileUploadResponseDto upload(
            MultipartFile file,
            String entityType,
            Long entityId
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File is empty");
        }

        if (entityType == null || entityType.isBlank()) {
            throw new BusinessRuleException("Entity type is required");
        }

        if (entityId == null || entityId <= 0) {
            throw new BusinessRuleException("Entity id is required");
        }

        String contentType = file.getContentType();

        List<String> allowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .toList();

        if (contentType == null || !allowed.contains(contentType)) {
            throw new BusinessRuleException(
                    "File type not allowed. Allowed: PDF, JPEG, PNG"
            );
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessRuleException("File size must be less than 10MB");
        }

        try {
            String safeEntityType = entityType.toLowerCase().trim();

            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();

            Path entityDir = root
                    .resolve(safeEntityType)
                    .resolve(entityId.toString())
                    .normalize();

            if (!entityDir.startsWith(root)) {
                throw new BusinessRuleException("Invalid upload path");
            }

            Files.createDirectories(entityDir);

            String originalName = file.getOriginalFilename();
            String extension = getExtension(originalName);
            String uniqueName = UUID.randomUUID() + "." + extension;

            Path targetPath = entityDir.resolve(uniqueName).normalize();

            if (!targetPath.startsWith(root)) {
                throw new BusinessRuleException("Invalid file path");
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String fileUrl =
                    "/api/files/" +
                            safeEntityType +
                            "/" +
                            entityId +
                            "/" +
                            uniqueName;

            return FileUploadResponseDto.builder()
                    .fileName(uniqueName)
                    .originalName(originalName)
                    .fileUrl(fileUrl)
                    .fileType(contentType)
                    .fileSize(file.getSize())
                    .entityType(safeEntityType.toUpperCase())
                    .entityId(entityId)
                    .build();

        } catch (IOException e) {
            throw new BusinessRuleException(
                    "Failed to upload file: " + e.getMessage()
            );
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path filePath = getFilePath(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessRuleException(
                    "Failed to delete file: " + e.getMessage()
            );
        }
    }

    @Override
    public Path getFilePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessRuleException("File path is required");
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();

        Path filePath = root
                .resolve(relativePath)
                .normalize();

        if (!filePath.startsWith(root)) {
            throw new BusinessRuleException("Invalid file path");
        }

        return filePath;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessRuleException("Invalid file name");
        }

        String extension =
                filename.substring(filename.lastIndexOf(".") + 1)
                        .toLowerCase();

        if (!List.of("pdf", "jpg", "jpeg", "png").contains(extension)) {
            throw new BusinessRuleException("File extension not allowed");
        }

        return extension;
    }
}
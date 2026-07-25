package com.nexaerp.fileupload;

import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileUploadService {

    FileUploadResponseDto upload(
            MultipartFile file,
            String entityType,
            Long entityId
    );

    void delete(String relativePath);

    Path getFilePath(String relativePath);
}
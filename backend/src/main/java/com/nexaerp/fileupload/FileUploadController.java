package com.nexaerp.fileupload;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    // Upload file
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId) {

        FileUploadResponseDto response =
                fileUploadService.upload(file, entityType, entityId);

        return ResponseEntity.ok(ApiResponse.success("File uploaded", response));
    }

    // Serve file (download/view)
    @GetMapping("/{entityType}/{entityId}/{fileName}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PathVariable String fileName) {

        try {
            Path filePath = fileUploadService.getFilePath(
                    entityType + "/" + entityId + "/" + fileName);

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Detect content type
            String lowerFileName = fileName.toLowerCase();

            String contentType = "application/octet-stream";

            if (lowerFileName.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lowerFileName.endsWith(".jpg")
                    || lowerFileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lowerFileName.endsWith(".png")) {
                contentType = "image/png";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Delete file
    @DeleteMapping("/{entityType}/{entityId}/{fileName}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PathVariable String fileName) {

        fileUploadService.delete(
                entityType + "/" + entityId + "/" + fileName);

        return ResponseEntity.ok(ApiResponse.success("File deleted", null));
    }
}
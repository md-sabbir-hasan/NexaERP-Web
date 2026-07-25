package com.nexaerp.fileupload.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponseDto {
    private String fileName;
    private String originalName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String entityType;
    private Long entityId;
}

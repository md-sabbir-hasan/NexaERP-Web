package com.nexaerp.audit.dto;

import com.nexaerp.audit.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTimelineItemDto {

    private Long id;
    private String entityName;
    private Long entityId;
    private AuditAction action;
    private String actorName;
    private String description;
    private LocalDateTime createdAt;
}

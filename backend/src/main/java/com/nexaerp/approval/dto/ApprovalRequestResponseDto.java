package com.nexaerp.approval.dto;

import com.nexaerp.approval.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class ApprovalRequestResponseDto {
    private Long id;
    private ApprovalEntityType entityType;
    private Long entityId;
    private String documentNumber;
    private String documentTitle;
    private String entityLabel;
    private String documentUrl;
    private Long makerUserId;
    private String makerName;
    private ApprovalStatus status;
    private String requiredPermission;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private Long decidedBy;
    private String decisionComment;
    private LocalDateTime consumedAt;
    private Long consumedBy;
    private Long supersedesRequestId;
    private boolean canDecide;
    private List<ApprovalActionResponseDto> actions;
}

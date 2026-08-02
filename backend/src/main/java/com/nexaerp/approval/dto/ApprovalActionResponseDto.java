package com.nexaerp.approval.dto;

import com.nexaerp.approval.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class ApprovalActionResponseDto {
    private Long id;
    private Long approvalRequestId;
    private ApprovalActionType action;
    private Long actorUserId;
    private String actorName;
    private ApprovalStatus fromStatus;
    private ApprovalStatus toStatus;
    private String comment;
    private LocalDateTime createdAt;
}

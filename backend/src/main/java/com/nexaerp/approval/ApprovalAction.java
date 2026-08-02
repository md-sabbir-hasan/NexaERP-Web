package com.nexaerp.approval;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_actions")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false, updatable = false)
    private ApprovalRequest approvalRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false)
    private ApprovalActionType action;
    @Column(nullable = false, updatable = false)
    private Long actorUserId;
    @Column(nullable = false, length = 150, updatable = false)
    private String actorNameSnapshot;
    @Enumerated(EnumType.STRING) @Column(length = 20, updatable = false)
    private ApprovalStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(length = 20, updatable = false)
    private ApprovalStatus toStatus;
    @Column(length = 500, updatable = false)
    private String comment;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}

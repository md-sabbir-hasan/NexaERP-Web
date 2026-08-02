package com.nexaerp.approval;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests", uniqueConstraints = @UniqueConstraint(
        name = "uk_approval_request_active", columnNames = {"entity_type", "entity_id", "active_marker"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "entity_type", nullable = false, length = 40)
    private ApprovalEntityType entityType;
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;
    @Column(name = "document_title", length = 255)
    private String documentTitle;
    @Column(name = "maker_user_id", nullable = false)
    private Long makerUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ApprovalStatus status;
    @Column(name = "required_permission", nullable = false, length = 100)
    private String requiredPermission;
    @Column(name = "document_updated_at", nullable = false)
    private LocalDateTime documentUpdatedAt;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private Long decidedBy;
    @Column(length = 500)
    private String decisionComment;
    private LocalDateTime consumedAt;
    private Long consumedBy;
    @Column(name = "active_marker")
    private Integer activeMarker;
    private Long supersedesRequestId;
    @Version
    private Long version;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}

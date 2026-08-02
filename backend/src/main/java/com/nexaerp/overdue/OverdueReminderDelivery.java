package com.nexaerp.overdue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "overdue_reminder_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_overdue_delivery_identity",
                columnNames = {
                        "document_type", "document_id", "milestone_days", "channel", "recipient_user_id"
                }
        ),
        indexes = {
                @Index(name = "idx_overdue_delivery_retry", columnList = "status,next_attempt_at"),
                @Index(name = "idx_overdue_delivery_document", columnList = "document_type,document_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueReminderDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private OverdueDocumentType documentType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "milestone_days", nullable = false)
    private Integer milestoneDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OverdueReminderChannel channel;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OverdueReminderStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = OverdueReminderStatus.PENDING;
        if (attemptCount == null) attemptCount = 0;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

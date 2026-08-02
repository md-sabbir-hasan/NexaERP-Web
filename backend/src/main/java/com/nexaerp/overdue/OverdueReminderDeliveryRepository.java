package com.nexaerp.overdue;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OverdueReminderDeliveryRepository
        extends JpaRepository<OverdueReminderDelivery, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO overdue_reminder_deliveries (
                document_type, document_id, milestone_days, channel, recipient_user_id,
                status, attempt_count, created_at, updated_at
            ) VALUES (
                :documentType, :documentId, :milestoneDays, :channel, :recipientUserId,
                'PENDING', 0, :now, :now
            )
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("documentType") String documentType,
            @Param("documentId") Long documentId,
            @Param("milestoneDays") Integer milestoneDays,
            @Param("channel") String channel,
            @Param("recipientUserId") Long recipientUserId,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OverdueReminderDelivery>
    findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
            OverdueDocumentType documentType,
            Long documentId,
            Integer milestoneDays,
            OverdueReminderChannel channel,
            Long recipientUserId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from OverdueReminderDelivery delivery where delivery.id = :id")
    Optional<OverdueReminderDelivery> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select delivery from OverdueReminderDelivery delivery
             where (delivery.status = com.nexaerp.overdue.OverdueReminderStatus.FAILED
                    and delivery.nextAttemptAt <= :now
                    and delivery.attemptCount < :maxAttempts)
                or (delivery.status = com.nexaerp.overdue.OverdueReminderStatus.PROCESSING
                    and delivery.processingStartedAt < :staleBefore
                    and delivery.attemptCount < :maxAttempts)
            """)
    Page<OverdueReminderDelivery> findRetryCandidates(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );
}

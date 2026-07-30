package com.nexaerp.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndUserId(
            Long id,
            Long userId
    );

    long countByUserIdAndReadAtIsNull(Long userId);

    boolean existsByUserIdAndTypeAndEntityTypeAndEntityId(
            Long userId,
            NotificationType type,
            String entityType,
            Long entityId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification notification
               set notification.readAt = :readAt,
                   notification.updatedAt = :readAt
             where notification.user.id = :userId
               and notification.readAt is null
            """)
    int markAllAsReadByUserId(
            @Param("userId") Long userId,
            @Param("readAt") LocalDateTime readAt
    );
}

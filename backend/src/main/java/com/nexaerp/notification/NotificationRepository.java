package com.nexaerp.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

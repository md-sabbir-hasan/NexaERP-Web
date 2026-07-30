package com.nexaerp.notification;

import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.common.response.PageResponseDto;
import com.nexaerp.notification.dto.NotificationResponseDto;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public PageResponseDto<NotificationResponseDto> getNotifications(
            int page,
            int size,
            boolean unreadOnly
    ) {
        Long userId = currentUserService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = unreadOnly
                ? notificationRepository
                        .findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return PageResponseDto.from(notifications.map(this::toResponse));
    }

    @Override
    public long getUnreadCount() {
        return notificationRepository.countByUserIdAndReadAtIsNull(
                currentUserService.getCurrentUserId()
        );
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long id) {
        Long userId = currentUserService.getCurrentUserId();

        Notification notification = notificationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found"
                ));

        if (!notification.isRead()) {
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        Long userId = currentUserService.getCurrentUserId();
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponseDto createForCurrentUser(
            NotificationType type,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    ) {
        return createForCurrentUser(
                type,
                NotificationPriority.MEDIUM,
                NotificationModule.SYSTEM,
                title,
                message,
                route,
                entityType,
                entityId
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponseDto createForCurrentUser(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    ) {
        return createForUser(
                currentUserService.getCurrentUserId(),
                type,
                priority,
                module,
                title,
                message,
                route,
                entityType,
                entityId,
                false
        );
    }

    @Override
    public void scheduleForCurrentUserAfterCommit(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    ) {
        scheduleAfterCommit(
                type, priority, module, title, message, route, entityType, entityId, false
        );
    }

    @Override
    public void scheduleUniqueForCurrentUserAfterCommit(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    ) {
        scheduleAfterCommit(
                type, priority, module, title, message, route, entityType, entityId, true
        );
    }

    private void scheduleAfterCommit(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId,
            boolean preventDuplicate
    ) {
        final Long userId;
        try {
            userId = currentUserService.getCurrentUserId();
        } catch (RuntimeException exception) {
            log.warn("Could not resolve notification recipient for type {} and entity {}:{}",
                    type, entityType, entityId, exception);
            return;
        }

        Runnable persistNotification = () -> {
            try {
                TransactionTemplate transactionTemplate =
                        new TransactionTemplate(transactionManager);

                transactionTemplate.setPropagationBehavior(
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW
                );

                transactionTemplate.executeWithoutResult(status ->
                        createForUser(
                                userId,
                                type,
                                priority,
                                module,
                                title,
                                message,
                                route,
                                entityType,
                                entityId,
                                preventDuplicate
                        )
                );

            } catch (RuntimeException exception) {
                log.warn(
                        "Business operation committed, but notification creation failed " +
                                "for type {} and entity {}:{}",
                        type,
                        entityType,
                        entityId,
                        exception
                );
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persistNotification.run();
                }
            });
            return;
        }

        persistNotification.run();
    }

    private NotificationResponseDto createForUser(
            Long userId,
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId,
            boolean preventDuplicate
    ) {
        if (preventDuplicate
                && entityId != null
                && entityType != null
                && notificationRepository.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                userId,
                type,
                entityType,
                entityId
        )) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .priority(priority != null ? priority : NotificationPriority.MEDIUM)
                .module(module != null ? module : NotificationModule.SYSTEM)
                .title(title)
                .message(message)
                .route(route)
                .entityType(entityType)
                .entityId(entityId)
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponseDto toResponse(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .priority(notification.getPriority() != null
                        ? notification.getPriority()
                        : NotificationPriority.MEDIUM)
                .module(notification.getModule() != null
                        ? notification.getModule()
                        : NotificationModule.SYSTEM)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .route(notification.getRoute())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .expiresAt(notification.getExpiresAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

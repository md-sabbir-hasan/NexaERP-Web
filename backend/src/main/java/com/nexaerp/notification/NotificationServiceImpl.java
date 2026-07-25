package com.nexaerp.notification;

import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.common.response.PageResponseDto;
import com.nexaerp.notification.dto.NotificationResponseDto;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

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
        LocalDateTime readAt = LocalDateTime.now();

        List<Notification> notifications = notificationRepository
                .findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                        userId,
                        Pageable.unpaged()
                )
                .getContent();

        notifications.forEach(notification -> notification.setReadAt(readAt));
        notificationRepository.saveAll(notifications);
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
        User user = userRepository.findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
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
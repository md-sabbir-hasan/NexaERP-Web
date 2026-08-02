package com.nexaerp.notification;

import com.nexaerp.common.response.PageResponseDto;
import com.nexaerp.notification.dto.NotificationResponseDto;

import java.util.Collection;

public interface NotificationService {

    PageResponseDto<NotificationResponseDto> getNotifications(
            int page,
            int size,
            boolean unreadOnly
    );

    long getUnreadCount();

    NotificationResponseDto markAsRead(Long id);

    void markAllAsRead();

    NotificationResponseDto createForCurrentUser(
            NotificationType type,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    );

    NotificationResponseDto createForCurrentUser(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    );

    void scheduleForCurrentUserAfterCommit(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    );

    void scheduleUniqueForCurrentUserAfterCommit(
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    );

    void scheduleUniqueForUserAfterCommit(
            Long userId,
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    );

    void scheduleUniqueForUsersAfterCommit(
            Collection<Long> userIds,
            NotificationType type,
            NotificationPriority priority,
            NotificationModule module,
            String title,
            String message,
            String route,
            String entityType,
            Long entityId
    );
}

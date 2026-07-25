package com.nexaerp.notification;

import com.nexaerp.common.response.PageResponseDto;
import com.nexaerp.notification.dto.NotificationResponseDto;

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
    );}
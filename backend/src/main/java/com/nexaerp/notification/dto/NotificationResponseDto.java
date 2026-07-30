package com.nexaerp.notification.dto;

import com.nexaerp.notification.NotificationType;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private Long id;
    private NotificationType type;
    private NotificationPriority priority;
    private NotificationModule module;
    private String title;
    private String message;
    private String route;
    private String entityType;
    private Long entityId;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

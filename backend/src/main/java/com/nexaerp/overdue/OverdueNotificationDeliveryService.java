package com.nexaerp.overdue;

import com.nexaerp.notification.Notification;
import com.nexaerp.notification.NotificationPriority;
import com.nexaerp.notification.NotificationRepository;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OverdueNotificationDeliveryService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deliver(Long recipientUserId, OverdueDocumentSnapshot document, String message) {
        User recipient = userRepository.findById(recipientUserId).orElse(null);
        if (recipient == null || recipient.getStatus() != UserStatus.ACTIVE) return false;

        notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(document.notificationType())
                .priority(NotificationPriority.HIGH)
                .module(document.notificationModule())
                .title(document.title())
                .message(message)
                .route(document.route())
                .entityType(document.entityType())
                .entityId(document.documentId())
                .build());
        return true;
    }
}

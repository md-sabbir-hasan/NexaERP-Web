package com.nexaerp.overdue;

import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OverdueDocumentSnapshot(
        OverdueDocumentType documentType,
        Long documentId,
        String documentNumber,
        String partyName,
        LocalDate dueDate,
        BigDecimal dueAmount,
        String currencyCode,
        Long creatorUserId,
        NotificationType notificationType,
        NotificationModule notificationModule,
        String title,
        String route,
        String entityType,
        long actualDaysOverdue
) {
}

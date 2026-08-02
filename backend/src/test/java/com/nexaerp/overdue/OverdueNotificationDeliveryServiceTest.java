package com.nexaerp.overdue;

import com.nexaerp.notification.Notification;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationPriority;
import com.nexaerp.notification.NotificationRepository;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueNotificationDeliveryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;

    @Test
    void activeCreatorReceivesExactInvoiceMetadata() {
        User user = User.builder().id(55L).status(UserStatus.ACTIVE).build();
        when(userRepository.findById(55L)).thenReturn(Optional.of(user));
        OverdueNotificationDeliveryService service =
                new OverdueNotificationDeliveryService(userRepository, notificationRepository);

        boolean sent = service.deliver(55L, invoice(),
                "Invoice INV-001 is 7 days overdue with BDT 100.00 outstanding.");

        assertTrue(sent);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification value = captor.getValue();
        assertTrue(value.getUser() == user);
        assertTrue(value.getType() == NotificationType.INVOICE_OVERDUE);
        assertTrue(value.getPriority() == NotificationPriority.HIGH);
        assertTrue(value.getModule() == NotificationModule.INVOICE);
        assertTrue(value.getTitle().equals("Invoice overdue"));
        assertTrue(value.getRoute().equals("/invoice/1"));
        assertTrue(value.getEntityType().equals("INVOICE"));
        assertTrue(value.getEntityId().equals(1L));
    }

    @Test
    void missingOrNonActiveCreatorIsSkipped() {
        OverdueNotificationDeliveryService service =
                new OverdueNotificationDeliveryService(userRepository, notificationRepository);
        when(userRepository.findById(55L)).thenReturn(Optional.empty());
        assertFalse(service.deliver(55L, invoice(), "message"));

        for (UserStatus status : new UserStatus[]{
                UserStatus.INACTIVE, UserStatus.LOCKED, UserStatus.PENDING}) {
            when(userRepository.findById(55L)).thenReturn(Optional.of(
                    User.builder().id(55L).status(status).build()));
            assertFalse(service.deliver(55L, invoice(), "message"));
        }
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private OverdueDocumentSnapshot invoice() {
        return new OverdueDocumentSnapshot(
                OverdueDocumentType.INVOICE, 1L, "INV-001", "Customer",
                LocalDate.of(2026, 8, 3), new BigDecimal("100.00"), "BDT", 55L,
                NotificationType.INVOICE_OVERDUE, NotificationModule.INVOICE,
                "Invoice overdue", "/invoice/1", "INVOICE", 7);
    }
}

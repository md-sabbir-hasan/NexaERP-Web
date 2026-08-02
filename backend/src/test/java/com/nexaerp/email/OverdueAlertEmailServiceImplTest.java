package com.nexaerp.email;

import com.nexaerp.email.dto.EmailDto;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.overdue.OverdueDocumentSnapshot;
import com.nexaerp.overdue.OverdueDocumentType;
import com.nexaerp.overdue.OverdueMessageFormatter;
import com.nexaerp.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueAlertEmailServiceImplTest {

    @Mock private EmailService emailService;
    @Mock private Environment environment;

    @Test
    void invoiceEmailUsesOnlyInternalRecipientAndSafeRequiredContent() {
        when(environment.getRequiredProperty("app.frontend.url"))
                .thenReturn("http://localhost:4200/");
        OverdueAlertEmailServiceImpl service = new OverdueAlertEmailServiceImpl(
                emailService, new OverdueMessageFormatter(), environment);
        User creator = User.builder().email(" creator@example.com ").build();

        service.send(creator, invoice());

        ArgumentCaptor<EmailDto> captor = ArgumentCaptor.forClass(EmailDto.class);
        verify(emailService).sendEmail(captor.capture());
        EmailDto email = captor.getValue();
        assertTrue(email.getTo().equals("creator@example.com"));
        assertTrue(email.getSubject().equals("Overdue invoice reminder — INV-001 — 7 days"));
        assertTrue(email.getBody().contains("Customer: Customer Name"));
        assertTrue(email.getBody().contains("Due date: 2026-08-03"));
        assertTrue(email.getBody().contains("Outstanding amount: BDT 100.50"));
        assertTrue(email.getBody().contains("http://localhost:4200/invoice/1"));
        assertTrue(email.getBody().contains("Confirm recent payments and credit notes"));
        assertFalse(email.getBody().contains("party@example.com"));
        assertFalse(email.getBody().contains("bank"));
    }

    private OverdueDocumentSnapshot invoice() {
        return new OverdueDocumentSnapshot(
                OverdueDocumentType.INVOICE, 1L, "INV-001\n", "Customer\nName",
                LocalDate.of(2026, 8, 3), new BigDecimal("100.5"), "BDT", 55L,
                NotificationType.INVOICE_OVERDUE, NotificationModule.INVOICE,
                "Invoice overdue", "/invoice/1", "INVOICE", 7);
    }
}

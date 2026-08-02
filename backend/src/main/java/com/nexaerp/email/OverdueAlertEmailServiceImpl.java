package com.nexaerp.email;

import com.nexaerp.email.dto.EmailDto;
import com.nexaerp.overdue.OverdueDocumentSnapshot;
import com.nexaerp.overdue.OverdueDocumentType;
import com.nexaerp.overdue.OverdueMessageFormatter;
import com.nexaerp.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OverdueAlertEmailServiceImpl implements OverdueAlertEmailService {

    private final EmailService emailService;
    private final OverdueMessageFormatter formatter;
    private final Environment environment;

    @Override
    public void send(User recipient, OverdueDocumentSnapshot document) {
        String documentLabel = document.documentType() == OverdueDocumentType.INVOICE
                ? "invoice"
                : "vendor bill";
        String subject = "Overdue " + documentLabel + " reminder — "
                + formatter.safe(document.documentNumber()) + " — "
                + document.actualDaysOverdue() + " days";

        String partyLabel = document.documentType() == OverdueDocumentType.INVOICE
                ? "Customer"
                : "Vendor";
        String advisory = document.documentType() == OverdueDocumentType.INVOICE
                ? "Confirm recent payments and credit notes before contacting the customer."
                : "Confirm recent payments and debit notes before taking action.";
        String frontendUrl = trimTrailingSlash(environment.getRequiredProperty("app.frontend.url"));

        String body = "A" + (document.documentType() == OverdueDocumentType.INVOICE ? "n " : " ")
                + documentLabel + " remains overdue.\n\n"
                + titleCase(documentLabel) + ": " + formatter.safe(document.documentNumber()) + "\n"
                + partyLabel + ": " + formatter.safe(document.partyName()) + "\n"
                + "Due date: " + document.dueDate() + "\n"
                + "Days overdue: " + document.actualDaysOverdue() + "\n"
                + "Outstanding amount: " + formatter.safe(document.currencyCode()) + " "
                + formatter.amount(document.dueAmount()) + "\n\n"
                + "Review " + documentLabel + ":\n"
                + frontendUrl + document.route() + "\n\n"
                + "This is an internal NexaERP reminder. " + advisory;

        emailService.sendEmail(EmailDto.builder()
                .to(recipient.getEmail().trim())
                .subject(subject)
                .body(body)
                .build());
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String titleCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

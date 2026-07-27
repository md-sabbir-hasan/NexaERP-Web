package com.nexaerp.email;

import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.email.dto.BudgetAlertEmailPayload;
import com.nexaerp.email.dto.EmailDto;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertEmailServiceImpl implements BudgetAlertEmailService {

    private final EmailService emailService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Value("${app.mail.budget-alerts.enabled:false}")
    private boolean enabled;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void scheduleAfterCommit(
            String documentType,
            Long documentId,
            String documentNumber,
            LocalDate postingDate,
            List<BudgetWarningDto> warnings
    ) {
        if (!enabled) {
            return;
        }

        List<BudgetWarningDto> uniqueWarnings = deduplicate(warnings);
        if (uniqueWarnings.isEmpty()) {
            return;
        }

        Long currentUserId;
        User recipient;
        try {
            currentUserId = currentUserService.getCurrentUserId();
            recipient = userRepository.findById(currentUserId).orElse(null);
        } catch (RuntimeException exception) {
            log.warn(
                    "Budget alert email skipped for document {} {} because the recipient could not be resolved",
                    safe(documentType),
                    safe(documentNumber),
                    exception
            );
            return;
        }

        if (recipient == null
                || recipient.getStatus() != UserStatus.ACTIVE
                || recipient.getEmail() == null
                || recipient.getEmail().isBlank()) {
            log.warn(
                    "Budget alert email skipped for document {} {} because recipient user {} is unavailable or inactive",
                    safe(documentType),
                    safe(documentNumber),
                    currentUserId
            );
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn(
                    "Budget alert email skipped for document {} {} because no active transaction synchronization exists",
                    safe(documentType),
                    safe(documentNumber)
            );
            return;
        }

        BudgetAlertEmailPayload payload = new BudgetAlertEmailPayload(
                recipient.getId(),
                recipient.getEmail().trim(),
                recipient.getName(),
                documentType,
                documentId,
                documentNumber,
                postingDate,
                uniqueWarnings
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendSafely(payload);
                    }
                }
        );
    }

    private void sendSafely(BudgetAlertEmailPayload payload) {
        try {
            emailService.sendEmail(EmailDto.builder()
                    .to(payload.recipientEmail())
                    .subject(buildSubject(payload))
                    .body(buildBody(payload))
                    .build());
        } catch (MailException exception) {
            log.warn(
                    "Budget alert email delivery failed for document {} {} and user {}",
                    safe(payload.documentType()),
                    safe(payload.documentNumber()),
                    payload.recipientUserId(),
                    exception
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Budget alert email delivery failed for document {} {} and user {}",
                    safe(payload.documentType()),
                    safe(payload.documentNumber()),
                    payload.recipientUserId(),
                    exception
            );
        }
    }

    private String buildSubject(BudgetAlertEmailPayload payload) {
        return "Budget exceeded - "
                + safe(payload.documentType())
                + " "
                + safe(payload.documentNumber());
    }

    private String buildBody(BudgetAlertEmailPayload payload) {
        StringBuilder body = new StringBuilder()
                .append("The posting succeeded but exceeded one or more budgets.\n\n")
                .append("Document type: ").append(safe(payload.documentType())).append('\n')
                .append("Document number: ").append(safe(payload.documentNumber())).append('\n')
                .append("Posting date: ").append(payload.postingDate()).append('\n')
                .append("Posted by: ").append(safe(payload.recipientName())).append("\n\n");

        for (BudgetWarningDto warning : payload.warnings()) {
            body.append("Account code: ").append(safe(warning.getAccountCode())).append('\n')
                    .append("Account name: ").append(safe(warning.getAccountName())).append('\n')
                    .append("Accounting period: ").append(safe(warning.getAccountingPeriodName())).append('\n')
                    .append("Budget amount: ").append(amount(warning.getBudgetAmount())).append('\n')
                    .append("Actual before posting: ").append(amount(warning.getActualBeforePosting())).append('\n')
                    .append("Transaction amount: ").append(amount(warning.getTransactionAmount())).append('\n')
                    .append("Projected actual: ").append(amount(warning.getProjectedActual())).append('\n')
                    .append("Exceeded amount: ").append(amount(warning.getExceededAmount())).append("\n\n");
        }

        Long budgetId = payload.warnings().get(0).getBudgetId();
        String route = budgetId == null
                ? "/budget"
                : "/budget/" + budgetId + "/variance";

        return body.append("Budget variance: ")
                .append(trimTrailingSlash(frontendUrl))
                .append(route)
                .append("\n\nThis is an advisory warning. The posting was not blocked.")
                .toString();
    }

    private List<BudgetWarningDto> deduplicate(List<BudgetWarningDto> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return List.of();
        }

        Map<WarningKey, BudgetWarningDto> unique = new LinkedHashMap<>();
        for (BudgetWarningDto warning : warnings) {
            if (warning != null) {
                unique.putIfAbsent(
                        new WarningKey(warning.getBudgetId(), warning.getAccountId()),
                        warning
                );
            }
        }
        return List.copyOf(unique.values());
    }

    private String safe(String value) {
        return value == null
                ? ""
                : value.replaceAll("[\\p{Cntrl}&&[^\\t]]", " ").trim();
    }

    private String amount(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record WarningKey(Long budgetId, Long accountId) {
    }
}

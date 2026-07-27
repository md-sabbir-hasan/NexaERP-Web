package com.nexaerp.email.dto;

import com.nexaerp.budget.dto.BudgetWarningDto;

import java.time.LocalDate;
import java.util.List;

public record BudgetAlertEmailPayload(
        Long recipientUserId,
        String recipientEmail,
        String recipientName,
        String documentType,
        Long documentId,
        String documentNumber,
        LocalDate postingDate,
        List<BudgetWarningDto> warnings
) {
    public BudgetAlertEmailPayload {
        warnings = warnings == null
                ? List.of()
                : warnings.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(BudgetAlertEmailPayload::copyWarning)
                        .toList();
    }

    private static BudgetWarningDto copyWarning(BudgetWarningDto warning) {
        return BudgetWarningDto.builder()
                .budgetId(warning.getBudgetId())
                .accountId(warning.getAccountId())
                .accountCode(warning.getAccountCode())
                .accountName(warning.getAccountName())
                .accountingPeriodId(warning.getAccountingPeriodId())
                .accountingPeriodName(warning.getAccountingPeriodName())
                .budgetAmount(warning.getBudgetAmount())
                .actualBeforePosting(warning.getActualBeforePosting())
                .transactionAmount(warning.getTransactionAmount())
                .projectedActual(warning.getProjectedActual())
                .exceededAmount(warning.getExceededAmount())
                .message(warning.getMessage())
                .build();
    }
}

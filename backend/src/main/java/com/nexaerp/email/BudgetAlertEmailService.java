package com.nexaerp.email;

import com.nexaerp.budget.dto.BudgetWarningDto;

import java.time.LocalDate;
import java.util.List;

public interface BudgetAlertEmailService {

    void scheduleAfterCommit(
            String documentType,
            Long documentId,
            String documentNumber,
            LocalDate postingDate,
            List<BudgetWarningDto> warnings
    );
}

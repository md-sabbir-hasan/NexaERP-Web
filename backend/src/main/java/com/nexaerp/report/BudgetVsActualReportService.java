package com.nexaerp.report;

import com.nexaerp.account.AccountType;
import com.nexaerp.report.dto.BudgetVsActualResponseDto;

import java.time.LocalDate;
import java.util.List;
import com.nexaerp.report.dto.BudgetVsActualOptionDto;

public interface BudgetVsActualReportService {
    BudgetVsActualResponseDto generate(
            Long budgetId, Long fromPeriodId, Long toPeriodId, AccountType accountType);

    BudgetVsActualResponseDto generateLegacy(
            Long budgetId, Long periodId, LocalDate fromDate, LocalDate toDate);

    List<BudgetVsActualOptionDto> getOptions();
}

package com.nexaerp.report.dto;

import com.nexaerp.budget.BudgetStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetVsActualResponseDto {
    private Long budgetId;
    private String budgetNumber;
    private String budgetName;
    private BudgetStatus budgetStatus;
    private Long fiscalYearId;
    private String fiscalYearName;
    private String currencyCode;
    private Long fromPeriodId;
    private Long toPeriodId;
    private List<Long> selectedPeriodIds;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalRevenueBudget;
    private BigDecimal totalRevenueActual;
    private BigDecimal totalRevenueVariance;
    private BigDecimal revenueAchievementPercent;
    private BigDecimal totalExpenseBudget;
    private BigDecimal totalExpenseActual;
    private BigDecimal totalExpenseVariance;
    private BigDecimal expenseUtilizationPercent;
    private List<BudgetVsActualLineDto> revenueLines;
    private List<BudgetVsActualLineDto> expenseLines;
    private LocalDateTime generatedAt;
}

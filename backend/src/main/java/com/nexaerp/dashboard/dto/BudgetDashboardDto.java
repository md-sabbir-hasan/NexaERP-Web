package com.nexaerp.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetDashboardDto {
    private boolean hasActiveBudget;
    private Long activeBudgetId;
    private String activeBudgetName;
    private String unavailableReason;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String currencyCode;

    private BigDecimal totalExpenseBudget;
    private BigDecimal totalExpenseActualYtd;
    private BigDecimal expenseUtilizationPercent;

    private BigDecimal totalRevenueBudget;
    private BigDecimal totalRevenueActualYtd;
    private BigDecimal revenueAchievementPercent;

    private List<BudgetTopAccountDto> topAccounts; // top 3 highest-utilization expense accounts
}

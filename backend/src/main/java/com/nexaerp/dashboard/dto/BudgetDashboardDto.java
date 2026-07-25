package com.nexaerp.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetDashboardDto {
    private boolean hasActiveBudget;
    private Long activeBudgetId;
    private String activeBudgetName;

    private BigDecimal totalExpenseBudget;
    private BigDecimal totalExpenseActualYtd;
    private BigDecimal expenseUtilizationPercent;

    private BigDecimal totalRevenueBudget;
    private BigDecimal totalRevenueActualYtd;
    private BigDecimal revenueAchievementPercent;

    private List<BudgetTopAccountDto> topAccounts; // top 3 highest-utilization expense accounts
}
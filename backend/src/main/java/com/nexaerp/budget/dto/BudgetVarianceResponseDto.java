package com.nexaerp.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetVarianceResponseDto {
    private Long budgetId;
    private String budgetName;

    private Long fiscalYearId;
    private String fiscalYearName;

    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal totalRevenueBudget;
    private BigDecimal totalRevenueActual;
    private BigDecimal totalExpenseBudget;
    private BigDecimal totalExpenseActual;

    private List<BudgetVarianceLineDto> lines;
}
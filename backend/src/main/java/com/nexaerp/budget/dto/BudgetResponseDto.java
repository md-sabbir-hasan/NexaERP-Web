package com.nexaerp.budget.dto;

import com.nexaerp.budget.BudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponseDto {
    private Long id;
    private String budgetNumber;

    private Long fiscalYearId;
    private String fiscalYearName;

    private Integer versionNumber;
    private Long revisedFromBudgetId;

    private String name;
    private String description;
    private BudgetStatus status;

    private BigDecimal totalRevenueBudget;
    private BigDecimal totalExpenseBudget;

    private LocalDateTime activatedAt;
    private LocalDateTime closedAt;

    private LocalDateTime createdAt;

    private List<BudgetLineResponseDto> lines;
}
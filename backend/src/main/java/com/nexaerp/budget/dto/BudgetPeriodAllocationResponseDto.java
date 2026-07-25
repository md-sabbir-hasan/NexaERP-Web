package com.nexaerp.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetPeriodAllocationResponseDto {
    private Long accountingPeriodId;
    private String periodName;
    private Integer periodNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budgetAmount;
}
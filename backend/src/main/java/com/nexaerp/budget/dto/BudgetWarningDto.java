package com.nexaerp.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetWarningDto {
    private Long budgetId;
    private Long accountId;
    private String accountCode;
    private String accountName;

    private Long accountingPeriodId;
    private String accountingPeriodName;

    private BigDecimal budgetAmount;
    private BigDecimal actualBeforePosting;
    private BigDecimal transactionAmount;
    private BigDecimal projectedActual;
    private BigDecimal exceededAmount;

    private String message;
}
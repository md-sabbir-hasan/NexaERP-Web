package com.nexaerp.budget.dto;

import com.nexaerp.budget.VarianceStatus;
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
public class BudgetVarianceLineDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountType; // EXPENSE / REVENUE

    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private BigDecimal varianceAmount;
    private BigDecimal variancePercent;
    private VarianceStatus varianceStatus;
    private BigDecimal utilizationPercent;
}
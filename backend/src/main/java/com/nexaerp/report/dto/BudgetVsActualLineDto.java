package com.nexaerp.report.dto;

import com.nexaerp.account.AccountType;
import com.nexaerp.budget.VarianceStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetVsActualLineDto {
    private Long budgetLineId;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private BigDecimal varianceAmount;
    private BigDecimal variancePercent;
    private BigDecimal utilizationPercent;
    private BigDecimal remainingAmount;
    private VarianceStatus varianceStatus;
}

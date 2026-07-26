package com.nexaerp.dashboard.dto;

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
public class BudgetTopAccountDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private BigDecimal utilizationPercent;
}

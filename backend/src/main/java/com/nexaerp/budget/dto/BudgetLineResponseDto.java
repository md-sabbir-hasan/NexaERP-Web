package com.nexaerp.budget.dto;

import com.nexaerp.budget.BudgetAllocationMethod;
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
public class BudgetLineResponseDto {
    private Long id;

    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountType;

    private BigDecimal annualAmount;
    private BudgetAllocationMethod allocationMethod;
    private String notes;

    private List<BudgetPeriodAllocationResponseDto> periodAllocations;
}
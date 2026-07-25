package com.nexaerp.budget.dto;

import com.nexaerp.budget.BudgetAllocationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLineRequestDto {

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotNull(message = "annualAmount is required")
    @DecimalMin(value = "0.01", message = "annualAmount must be greater than 0")
    private BigDecimal annualAmount;

    @NotNull(message = "allocationMethod is required")
    private BudgetAllocationMethod allocationMethod;

    // Required only when allocationMethod = MANUAL
    @Valid
    private List<BudgetPeriodAmountRequestDto> periodAmounts;

    private String notes;
}
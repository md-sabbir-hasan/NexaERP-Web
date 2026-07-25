package com.nexaerp.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPeriodAmountRequestDto {

    @NotNull(message = "accountingPeriodId is required")
    private Long accountingPeriodId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.00", message = "amount cannot be negative")
    private BigDecimal amount;
}
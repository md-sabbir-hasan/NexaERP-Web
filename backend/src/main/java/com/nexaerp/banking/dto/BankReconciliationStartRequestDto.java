package com.nexaerp.banking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliationStartRequestDto {

    @NotNull(message = "Bank account is required")
    private Long bankAccountId;

    @NotNull(message = "Statement date is required")
    private LocalDate statementDate;

    @NotNull(message = "Statement closing balance is required")
    private BigDecimal statementBalance;

    private String notes;
}

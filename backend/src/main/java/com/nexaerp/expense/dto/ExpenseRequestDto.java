package com.nexaerp.expense.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class ExpenseRequestDto {

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Expense category account is required")
    private Long expenseAccountId;

    // true = Pay Now, false = Pay Later
    @NotNull(message = "paidImmediately is required")
    private Boolean paidImmediately;

    // Required when paidImmediately = true (Cash/Bank/bKash account)
    private Long paymentAccountId;

    // Required when paidImmediately = false (who to pay later). Optional otherwise.
    private Long partyId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String referenceNumber;
    private String attachmentUrl;
    private String notes;
}
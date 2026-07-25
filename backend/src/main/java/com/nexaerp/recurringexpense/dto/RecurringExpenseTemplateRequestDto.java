package com.nexaerp.recurringexpense.dto;

import com.nexaerp.recurringexpense.RecurringFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class RecurringExpenseTemplateRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "expenseAccountId is required")
    private Long expenseAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "paidImmediately is required")
    private Boolean paidImmediately;

    private Long paymentAccountId; // required when paidImmediately = true
    private Long partyId;          // required when paidImmediately = false

    @NotNull(message = "frequency is required")
    private RecurringFrequency frequency;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private String referenceNumber;
    private String notes;
}
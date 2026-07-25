package com.nexaerp.recurringexpense.dto;

import com.nexaerp.recurringexpense.RecurringExpenseStatus;
import com.nexaerp.recurringexpense.RecurringFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExpenseTemplateResponseDto {
    private Long id;
    private String name;

    private Long expenseAccountId;
    private String expenseAccountName;

    private BigDecimal amount;
    private Boolean paidImmediately;

    private Long paymentAccountId;
    private String paymentAccountName;

    private Long partyId;
    private String partyName;

    private RecurringFrequency frequency;
    private LocalDate startDate;
    private LocalDate nextRunDate;
    private LocalDate endDate;
    private LocalDate lastGeneratedDate;
    private Long lastGeneratedExpenseId;

    private RecurringExpenseStatus status;
    private String referenceNumber;
    private String notes;
    private String lastRunError;
}
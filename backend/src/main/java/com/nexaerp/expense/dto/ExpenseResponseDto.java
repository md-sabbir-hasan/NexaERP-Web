package com.nexaerp.expense.dto;

import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.expense.ExpensePaymentStatus;
import com.nexaerp.expense.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponseDto {
    private Long id;
    private String expenseNumber;
    private LocalDate expenseDate;

    private Long expenseAccountId;
    private String expenseAccountName;

    private Boolean paidImmediately;

    private Long paymentAccountId;
    private String paymentAccountName;

    private Long partyId;
    private String partyName;

    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private ExpensePaymentStatus paymentStatus;

    private String referenceNumber;
    private String attachmentUrl;
    private String notes;

    private ExpenseStatus status;
    private LocalDateTime cancelledAt;
    private String cancelReason;

    private LocalDateTime createdAt;

    //budget
    private List<BudgetWarningDto> budgetWarnings;

    private Long recurringTemplateId;
}
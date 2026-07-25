package com.nexaerp.banking.dto;

import com.nexaerp.banking.enums.ReconciliationStatus;
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
public class BankReconciliationResponseDto {
    private Long id;
    private Long bankAccountId;
    private String bankAccountName;
    private LocalDate statementDate;
    private BigDecimal statementBalance;

    // Book balance as of statementDate (per system ledger)
    private BigDecimal bookBalance;

    // Reconciling items: recorded in books, not yet cleared by the bank
    private BigDecimal depositsInTransit;   // unreconciled CREDIT txns
    private BigDecimal outstandingCheques;  // unreconciled DEBIT txns

    // statementBalance + depositsInTransit - outstandingCheques  (should equal bookBalance)
    private BigDecimal adjustedBankBalance;

    // bookBalance - adjustedBankBalance ; must be 0 to complete
    private BigDecimal difference;

    private ReconciliationStatus status;
    private String notes;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    private List<Long> unmatchedTransactionIds;
}

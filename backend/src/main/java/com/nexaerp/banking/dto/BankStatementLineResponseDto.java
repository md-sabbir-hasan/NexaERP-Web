package com.nexaerp.banking.dto;

import com.nexaerp.banking.enums.StatementLineStatus;
import com.nexaerp.banking.enums.TransactionType;
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
public class BankStatementLineResponseDto {
    private Long id;
    private Long reconciliationId;
    private LocalDate lineDate;
    private String description;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String referenceNumber;
    private StatementLineStatus status;
    private Long matchedTransactionId;
    private String matchedTransactionNumber;
}

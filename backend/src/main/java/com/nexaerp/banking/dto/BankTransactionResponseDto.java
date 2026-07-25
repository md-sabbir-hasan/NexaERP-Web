package com.nexaerp.banking.dto;


import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionResponseDto {
    private Long id;
    private String transactionNumber;
    private Long bankAccountId;
    private String bankAccountName;
    private LocalDate transactionDate;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private String referenceNumber;
    private Long contraAccountId;
    private String contraAccountName;
    private Boolean reconciled;
    private LocalDateTime reconciledAt;
    private Boolean voided;
    private LocalDateTime voidedAt;
    private TransactionSourceType sourceType;
    private LocalDateTime createdAt;
}

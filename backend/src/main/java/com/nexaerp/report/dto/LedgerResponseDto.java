package com.nexaerp.report.dto;

import com.nexaerp.account.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerResponseDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<LedgerEntryDto> entries;
}

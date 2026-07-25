package com.nexaerp.report.dto;

import com.nexaerp.account.AccountType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBalanceRowDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private AccountType accountType;
    private BigDecimal debitBalance;
    private BigDecimal creditBalance;
}

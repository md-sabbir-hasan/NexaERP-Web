package com.nexaerp.banking.dto;

import com.nexaerp.banking.enums.BankAccountType;
import com.nexaerp.banking.enums.WalletProvider;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountResponseDto {
    private Long id;
    private String accountName;
    private String accountNumber;
    private String bankName;
    private String branchName;
    private BankAccountType accountType;
    private String currency;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private Boolean isActive;
    private String notes;
    private String mobileNumber;
    private WalletProvider walletProvider;
    private Long coaAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

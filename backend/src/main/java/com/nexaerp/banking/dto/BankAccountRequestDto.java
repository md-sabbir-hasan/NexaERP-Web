package com.nexaerp.banking.dto;

import com.nexaerp.banking.enums.BankAccountType;
import com.nexaerp.banking.enums.WalletProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountRequestDto {
    @NotBlank(message = "Account name is required")
    private String accountName;

    private String accountNumber;
    private String bankName;
    private String branchName;

    @NotNull(message = "Account type is required")
    private BankAccountType accountType;

    private String currency;
    private BigDecimal openingBalance;
    private String notes;

    // Mobile Wallet specific
    private String mobileNumber;
    private WalletProvider walletProvider;

    // COA account link
    private Long coaAccountId;
}

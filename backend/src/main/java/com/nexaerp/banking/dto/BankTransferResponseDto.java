package com.nexaerp.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransferResponseDto {
    private BankTransactionResponseDto debitTransaction;   // leg on the "from" account
    private BankTransactionResponseDto creditTransaction;  // leg on the "to" account
}
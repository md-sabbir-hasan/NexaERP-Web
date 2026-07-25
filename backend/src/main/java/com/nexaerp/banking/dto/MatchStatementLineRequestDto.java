package com.nexaerp.banking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchStatementLineRequestDto {
    @NotNull(message = "Transaction id is required")
    private Long transactionId;
}

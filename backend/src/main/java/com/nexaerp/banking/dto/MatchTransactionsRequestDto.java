package com.nexaerp.banking.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchTransactionsRequestDto {

    @NotEmpty(message = "At least one transaction id is required")
    private List<Long> transactionIds;
}

package com.nexaerp.journal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalLineRequestDto {
    @NotNull(message = "Account is required")
    private Long accountId;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal debit = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal credit = BigDecimal.ZERO;

    private String description;
}

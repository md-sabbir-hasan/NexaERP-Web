package com.nexaerp.currency.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyConversionDto {

    private String fromCurrency;
    private String toCurrency;

    private BigDecimal originalAmount;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;

    private LocalDate effectiveDate;
}
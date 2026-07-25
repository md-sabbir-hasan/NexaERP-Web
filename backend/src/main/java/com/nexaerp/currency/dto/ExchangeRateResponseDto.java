package com.nexaerp.currency.dto;

import com.nexaerp.currency.enums.RateSource;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateResponseDto {

    private Long id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDate effectiveDate;
    private RateSource source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
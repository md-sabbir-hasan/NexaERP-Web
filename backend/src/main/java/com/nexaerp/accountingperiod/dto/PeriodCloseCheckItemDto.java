package com.nexaerp.accountingperiod.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodCloseCheckItemDto {
    private Long id;
    private String reference;
    private LocalDate date;
    private BigDecimal amount;
}
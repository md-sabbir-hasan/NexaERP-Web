package com.nexaerp.currency.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyResponseDto {

    private Long id;
    private String code;
    private String name;
    private String symbol;
    private Integer decimalPlaces;
    private Boolean active;
    private Boolean baseCurrency;
}
package com.nexaerp.currency.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrencyRequestDto {

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must contain 3 characters")
    private String code;

    @NotBlank(message = "Currency name is required")
    private String name;

    private String symbol;

    @Min(value = 0, message = "Decimal places cannot be negative")
    @Max(value = 6, message = "Decimal places cannot exceed 6")
    private Integer decimalPlaces = 2;

    private Boolean active = true;

    private Boolean baseCurrency = false;
}
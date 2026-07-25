package com.nexaerp.currency.dto;

import com.nexaerp.currency.enums.RateSource;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExchangeRateRequestDto {

    @NotBlank(message = "From currency is required")
    private String fromCurrency;

    private String toCurrency;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(
            value = "0.00000001",
            message = "Exchange rate must be greater than zero"
    )
    private BigDecimal rate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private RateSource source = RateSource.MANUAL;
}
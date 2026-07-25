package com.nexaerp.currency.service;

import com.nexaerp.currency.dto.CurrencyConversionDto;
import com.nexaerp.currency.dto.ExchangeRateRequestDto;
import com.nexaerp.currency.dto.ExchangeRateResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateService {

    ExchangeRateResponseDto create(
            ExchangeRateRequestDto request
    );

    ExchangeRateResponseDto update(
            Long id,
            ExchangeRateRequestDto request
    );

    ExchangeRateResponseDto getById(Long id);

    ExchangeRateResponseDto getLatestRate(
            String fromCurrency,
            String toCurrency
    );

    ExchangeRateResponseDto getRateForDate(
            String fromCurrency,
            String toCurrency,
            LocalDate date
    );

    List<ExchangeRateResponseDto> getHistory(
            String fromCurrency,
            String toCurrency
    );

    List<ExchangeRateResponseDto> getAll();

    BigDecimal getRateValue(
            String fromCurrency,
            String toCurrency,
            LocalDate date
    );

    BigDecimal convertAmount(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            LocalDate date
    );

    CurrencyConversionDto convert(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            LocalDate date
    );

    void delete(Long id);
}
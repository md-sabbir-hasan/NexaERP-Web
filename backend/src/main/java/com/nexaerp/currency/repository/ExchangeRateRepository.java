package com.nexaerp.currency.repository;

import com.nexaerp.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository
        extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate>
    findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseOrderByEffectiveDateDesc(
            String fromCurrency,
            String toCurrency
    );

    Optional<ExchangeRate>
    findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String fromCurrency,
            String toCurrency,
            LocalDate effectiveDate
    );

    List<ExchangeRate>
    findByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseOrderByEffectiveDateDesc(
            String fromCurrency,
            String toCurrency
    );

    boolean existsByFromCurrencyIdAndToCurrencyIdAndEffectiveDate(
            Long fromCurrencyId,
            Long toCurrencyId,
            LocalDate effectiveDate
    );
}
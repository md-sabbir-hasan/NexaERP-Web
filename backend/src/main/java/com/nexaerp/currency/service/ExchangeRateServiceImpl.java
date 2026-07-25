package com.nexaerp.currency.service;

import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.currency.dto.CurrencyConversionDto;
import com.nexaerp.currency.dto.ExchangeRateRequestDto;
import com.nexaerp.currency.dto.ExchangeRateResponseDto;
import com.nexaerp.currency.entity.Currency;
import com.nexaerp.currency.entity.ExchangeRate;
import com.nexaerp.currency.repository.CurrencyRepository;
import com.nexaerp.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final int RATE_SCALE = 8;
    private static final int AMOUNT_SCALE = 2;

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;

    /*
     * Create exchange rate
     */
    @Override
    public ExchangeRateResponseDto create(
            ExchangeRateRequestDto request
    ) {
        Currency fromCurrency =
                getActiveCurrency(request.getFromCurrency());

        Currency toCurrency =
                getActiveCurrency(resolveToCurrency(request));

        validateCurrencyPair(fromCurrency, toCurrency);

        boolean alreadyExists =
                exchangeRateRepository
                        .existsByFromCurrencyIdAndToCurrencyIdAndEffectiveDate(
                                fromCurrency.getId(),
                                toCurrency.getId(),
                                request.getEffectiveDate()
                        );

        if (alreadyExists) {
            throw new BusinessRuleException(
                    "Exchange rate already exists for "
                            + fromCurrency.getCode()
                            + " to "
                            + toCurrency.getCode()
                            + " on "
                            + request.getEffectiveDate()
            );
        }

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(normalizeRate(request.getRate()))
                .effectiveDate(request.getEffectiveDate())
                .source(request.getSource())
                .build();

        ExchangeRate saved =
                exchangeRateRepository.save(exchangeRate);

        return toResponse(saved);
    }

    /*
     * Update exchange rate
     */
    @Override
    public ExchangeRateResponseDto update(
            Long id,
            ExchangeRateRequestDto request
    ) {
        ExchangeRate exchangeRate = getEntity(id);

        Currency fromCurrency =
                getActiveCurrency(request.getFromCurrency());

        Currency toCurrency =
                getActiveCurrency(resolveToCurrency(request));

        validateCurrencyPair(fromCurrency, toCurrency);

        exchangeRateRepository
                .findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        fromCurrency.getCode(),
                        toCurrency.getCode(),
                        request.getEffectiveDate()
                )
                .filter(existing ->
                        existing.getEffectiveDate()
                                .equals(request.getEffectiveDate())
                                && !existing.getId().equals(id)
                )
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "Exchange rate already exists for this date"
                    );
                });

        exchangeRate.setFromCurrency(fromCurrency);
        exchangeRate.setToCurrency(toCurrency);
        exchangeRate.setRate(
                normalizeRate(request.getRate())
        );
        exchangeRate.setEffectiveDate(
                request.getEffectiveDate()
        );
        exchangeRate.setSource(request.getSource());

        ExchangeRate updated =
                exchangeRateRepository.save(exchangeRate);

        return toResponse(updated);
    }

    /*
     * Get exchange rate by ID
     */
    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getById(Long id) {
        return toResponse(getEntity(id));
    }

    /*
     * Get the latest applicable exchange rate.
     *
     * Important:
     * Future-dated rates will not be returned.
     */
    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getLatestRate(
            String fromCurrency,
            String toCurrency
    ) {
        return getRateForDate(
                fromCurrency,
                toCurrency,
                LocalDate.now()
        );
    }

    /*
     * Get applicable exchange rate for a specific date.
     *
     * Resolution order:
     * 1. Same currency = 1
     * 2. Direct exchange rate
     * 3. Reverse exchange rate
     */
    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponseDto getRateForDate(
            String fromCurrency,
            String toCurrency,
            LocalDate date
    ) {
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);

        LocalDate rateDate =
                date != null ? date : LocalDate.now();

        if (from.equals(to)) {
            return buildSameCurrencyRate(
                    from,
                    rateDate
            );
        }

        /*
         * First try direct rate:
         *
         * Example:
         * USD -> BDT
         */
        Optional<ExchangeRate> directRate =
                exchangeRateRepository
                        .findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                                from,
                                to,
                                rateDate
                        );

        if (directRate.isPresent()) {
            return toResponse(directRate.get());
        }

        /*
         * If direct rate is unavailable, try reverse:
         *
         * Requested:
         * BDT -> USD
         *
         * Stored:
         * USD -> BDT
         *
         * Result:
         * 1 / stored rate
         */
        Optional<ExchangeRate> reverseRate =
                exchangeRateRepository
                        .findTopByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                                to,
                                from,
                                rateDate
                        );

        if (reverseRate.isPresent()) {
            return buildReverseRateResponse(
                    from,
                    to,
                    reverseRate.get()
            );
        }

        throw new ResourceNotFoundException(
                "No exchange rate found from "
                        + from
                        + " to "
                        + to
                        + " on or before "
                        + rateDate
        );
    }

    /*
     * Get direct exchange-rate history.
     *
     * This returns only the stored direction.
     * Example: USD -> BDT
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getHistory(
            String fromCurrency,
            String toCurrency
    ) {
        return exchangeRateRepository
                .findByFromCurrencyCodeIgnoreCaseAndToCurrencyCodeIgnoreCaseOrderByEffectiveDateDesc(
                        normalize(fromCurrency),
                        normalize(toCurrency)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get all exchange rates
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponseDto> getAll() {
        return exchangeRateRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Return only the exchange-rate value
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRateValue(
            String fromCurrency,
            String toCurrency,
            LocalDate date
    ) {
        return getRateForDate(
                fromCurrency,
                toCurrency,
                date
        ).getRate();
    }

    /*
     * Convert and return only converted amount
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal convertAmount(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            LocalDate date
    ) {
        validateAmount(amount);

        BigDecimal rate = getRateValue(
                fromCurrency,
                toCurrency,
                date
        );

        return amount
                .multiply(rate)
                .setScale(
                        AMOUNT_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * Convert and return complete conversion information
     */
    @Override
    @Transactional(readOnly = true)
    public CurrencyConversionDto convert(
            String fromCurrency,
            String toCurrency,
            BigDecimal amount,
            LocalDate date
    ) {
        validateAmount(amount);

        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);

        LocalDate rateDate =
                date != null ? date : LocalDate.now();

        ExchangeRateResponseDto applicableRate =
                getRateForDate(
                        from,
                        to,
                        rateDate
                );

        BigDecimal rate =
                applicableRate.getRate();

        BigDecimal convertedAmount =
                amount.multiply(rate)
                        .setScale(
                                AMOUNT_SCALE,
                                RoundingMode.HALF_UP
                        );

        return CurrencyConversionDto.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .originalAmount(amount)
                .exchangeRate(rate)
                .convertedAmount(convertedAmount)
                .effectiveDate(
                        applicableRate.getEffectiveDate()
                )
                .build();
    }

    /*
     * Delete exchange rate
     */
    @Override
    public void delete(Long id) {
        ExchangeRate exchangeRate = getEntity(id);

        exchangeRateRepository.delete(exchangeRate);
    }

    /*
     * Find active currency by code
     */
    private Currency getActiveCurrency(String code) {
        String normalizedCode = normalize(code);

        Currency currency =
                currencyRepository
                        .findByCodeIgnoreCase(normalizedCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Currency not found: "
                                                + normalizedCode
                                )
                        );

        if (!Boolean.TRUE.equals(currency.getActive())) {
            throw new BusinessRuleException(
                    "Currency is inactive: "
                            + normalizedCode
            );
        }

        return currency;
    }

    /*
     * Find exchange-rate entity by ID
     */
    private ExchangeRate getEntity(Long id) {
        return exchangeRateRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Exchange rate not found with id: "
                                        + id
                        )
                );
    }

    /*
     * From currency and to currency cannot be the same
     * while saving exchange-rate records.
     */
    private void validateCurrencyPair(
            Currency fromCurrency,
            Currency toCurrency
    ) {
        if (fromCurrency.getId()
                .equals(toCurrency.getId())) {
            throw new BusinessRuleException(
                    "From currency and to currency cannot be same"
            );
        }
    }

    /*
     * Conversion amount validation
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessRuleException(
                    "Amount is required"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "Amount cannot be negative"
            );
        }
    }

    /*
     * Use request to-currency when provided.
     * Otherwise use configured base currency.
     */
    private String resolveToCurrency(
            ExchangeRateRequestDto request
    ) {
        if (request.getToCurrency() != null
                && !request.getToCurrency().isBlank()) {
            return request.getToCurrency();
        }

        return currencyRepository
                .findByBaseCurrencyTrue()
                .map(Currency::getCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Base currency is not configured"
                        )
                );
    }

    /*
     * Normalize exchange-rate precision
     */
    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            throw new BusinessRuleException(
                    "Exchange rate is required"
            );
        }

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "Exchange rate must be greater than zero"
            );
        }

        return rate.setScale(
                RATE_SCALE,
                RoundingMode.HALF_UP
        );
    }

    /*
     * Normalize currency code
     */
    private String normalize(String currencyCode) {
        if (currencyCode == null
                || currencyCode.isBlank()) {
            throw new BusinessRuleException(
                    "Currency code is required"
            );
        }

        return currencyCode
                .trim()
                .toUpperCase();
    }

    /*
     * Same-currency response
     *
     * Example:
     * BDT -> BDT = 1
     */
    private ExchangeRateResponseDto buildSameCurrencyRate(
            String currencyCode,
            LocalDate date
    ) {
        String code = normalize(currencyCode);

        return ExchangeRateResponseDto.builder()
                .fromCurrency(code)
                .toCurrency(code)
                .rate(
                        BigDecimal.ONE.setScale(
                                RATE_SCALE,
                                RoundingMode.HALF_UP
                        )
                )
                .effectiveDate(date)
                .build();
    }

    /*
     * Create a calculated reverse-rate response.
     *
     * Example:
     * Stored USD -> BDT = 120
     * Requested BDT -> USD = 1 / 120
     */
    private ExchangeRateResponseDto buildReverseRateResponse(
            String requestedFromCurrency,
            String requestedToCurrency,
            ExchangeRate storedReverseRate
    ) {
        BigDecimal calculatedRate =
                BigDecimal.ONE.divide(
                        storedReverseRate.getRate(),
                        RATE_SCALE,
                        RoundingMode.HALF_UP
                );

        return ExchangeRateResponseDto.builder()
                .id(null)
                .fromCurrency(requestedFromCurrency)
                .toCurrency(requestedToCurrency)
                .rate(calculatedRate)
                .effectiveDate(
                        storedReverseRate.getEffectiveDate()
                )
                .source(storedReverseRate.getSource())
                .createdAt(storedReverseRate.getCreatedAt())
                .updatedAt(storedReverseRate.getUpdatedAt())
                .build();
    }

    /*
     * Map entity to response DTO
     */
    private ExchangeRateResponseDto toResponse(
            ExchangeRate exchangeRate
    ) {
        return ExchangeRateResponseDto.builder()
                .id(exchangeRate.getId())
                .fromCurrency(
                        exchangeRate
                                .getFromCurrency()
                                .getCode()
                )
                .toCurrency(
                        exchangeRate
                                .getToCurrency()
                                .getCode()
                )
                .rate(exchangeRate.getRate())
                .effectiveDate(
                        exchangeRate.getEffectiveDate()
                )
                .source(exchangeRate.getSource())
                .createdAt(exchangeRate.getCreatedAt())
                .updatedAt(exchangeRate.getUpdatedAt())
                .build();
    }
}
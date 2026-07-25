package com.nexaerp.currency.service;

import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.currency.dto.CurrencyRequestDto;
import com.nexaerp.currency.dto.CurrencyResponseDto;
import com.nexaerp.currency.entity.Currency;
import com.nexaerp.currency.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Override
    public CurrencyResponseDto create(CurrencyRequestDto request) {

        String code = normalize(request.getCode());

        if (currencyRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException(
                    "Currency already exists: " + code
            );
        }

        if (Boolean.TRUE.equals(request.getBaseCurrency())) {
            removeExistingBaseCurrency();
        }

        Currency currency = Currency.builder()
                .code(code)
                .name(request.getName().trim())
                .symbol(request.getSymbol())
                .decimalPlaces(
                        request.getDecimalPlaces() != null
                                ? request.getDecimalPlaces()
                                : 2
                )
                .active(
                        request.getActive() == null
                                || request.getActive()
                )
                .baseCurrency(
                        Boolean.TRUE.equals(request.getBaseCurrency())
                )
                .build();

        return toResponse(currencyRepository.save(currency));
    }

    @Override
    public CurrencyResponseDto update(
            Long id,
            CurrencyRequestDto request
    ) {

        Currency currency = getEntity(id);
        String code = normalize(request.getCode());

        currencyRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "Currency already exists: " + code
                    );
                });

        if (Boolean.TRUE.equals(request.getBaseCurrency())
                && !Boolean.TRUE.equals(currency.getBaseCurrency())) {
            removeExistingBaseCurrency();
        }

        currency.setCode(code);
        currency.setName(request.getName().trim());
        currency.setSymbol(request.getSymbol());
        currency.setDecimalPlaces(request.getDecimalPlaces());
        currency.setActive(request.getActive());
        currency.setBaseCurrency(request.getBaseCurrency());

        return toResponse(currencyRepository.save(currency));
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponseDto getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponseDto getByCode(String code) {
        return currencyRepository.findByCodeIgnoreCase(code)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Currency not found: " + code
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponseDto getBaseCurrency() {
        return currencyRepository.findByBaseCurrencyTrue()
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Base currency is not configured"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyResponseDto> getAll() {
        return currencyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyResponseDto> getActive() {
        return currencyRepository.findByActiveTrueOrderByCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void setBaseCurrency(Long id) {

        Currency currency = getEntity(id);

        if (!Boolean.TRUE.equals(currency.getActive())) {
            throw new BusinessRuleException(
                    "Inactive currency cannot be set as base currency"
            );
        }

        removeExistingBaseCurrency();

        currency.setBaseCurrency(true);
        currencyRepository.save(currency);
    }

    @Override
    public void deactivate(Long id) {

        Currency currency = getEntity(id);

        if (Boolean.TRUE.equals(currency.getBaseCurrency())) {
            throw new BusinessRuleException(
                    "Base currency cannot be deactivated"
            );
        }

        currency.setActive(false);
        currencyRepository.save(currency);
    }

    private void removeExistingBaseCurrency() {
        currencyRepository.findByBaseCurrencyTrue()
                .ifPresent(existing -> {
                    existing.setBaseCurrency(false);
                    currencyRepository.save(existing);
                });
    }

    private Currency getEntity(Long id) {
        return currencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Currency not found with id: " + id
                ));
    }

    private String normalize(String code) {
        return code.trim().toUpperCase();
    }

    private CurrencyResponseDto toResponse(Currency currency) {
        return CurrencyResponseDto.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .decimalPlaces(currency.getDecimalPlaces())
                .active(currency.getActive())
                .baseCurrency(currency.getBaseCurrency())
                .build();
    }
}
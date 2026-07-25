package com.nexaerp.currency.service;

import com.nexaerp.currency.dto.CurrencyRequestDto;
import com.nexaerp.currency.dto.CurrencyResponseDto;

import java.util.List;

public interface CurrencyService {

    CurrencyResponseDto create(CurrencyRequestDto request);

    CurrencyResponseDto update(Long id, CurrencyRequestDto request);

    CurrencyResponseDto getById(Long id);

    CurrencyResponseDto getByCode(String code);

    CurrencyResponseDto getBaseCurrency();

    List<CurrencyResponseDto> getAll();

    List<CurrencyResponseDto> getActive();

    void setBaseCurrency(Long id);

    void deactivate(Long id);
}